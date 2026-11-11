package com.example.statistics;

import com.example.mapper.StatisticsMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 显式启用；只连接独立开发库，全部夹具在一个事务内回滚，禁止连接稳定库。 */
@EnabledIfEnvironmentVariable(named="CAMPUS_STATS_DB_PASSWORD",matches=".+")
class StatisticsDatabaseTest {
    Connection connection;
    long insert(String sql,Object... args) throws SQLException {
        try(var statement=connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for(int i=0;i<args.length;i++) statement.setObject(i+1,args[i]); statement.executeUpdate();
            try(var keys=statement.getGeneratedKeys()) { return keys.next()?keys.getLong(1):0; }
        }
    }
    long user(String suffix,String created,boolean ordinary,boolean admin,int status) throws SQLException {
        long id=insert("INSERT INTO base_user(account,username,login_status,create_time) VALUES(?,?,?,?)","stats_"+UUID.randomUUID().toString().substring(0,12),suffix,status,created);
        if(ordinary) insert("INSERT INTO `user`(base_user_id) VALUES(?)",id);
        if(admin) insert("INSERT INTO admin(base_user_id) VALUES(?)",id);
        return id;
    }
    long product(long user,long category,String created) throws SQLException { return insert("INSERT INTO product(user_id,category_id,title,price,status,create_time) VALUES(?,?,'统计测试夹具',10,1,?)",user,category,created); }
    long order(long product,long buyer,int state,int quantity,String amount,String created) throws SQLException {
        return insert("INSERT INTO `order`(order_no,product_id,buyer_id,seller_id,quantity,unit_price,total_amount,shipping_fee,pay_amount,order_status,create_time) VALUES(?,?,?,?,?,10,?,1.25,?,?,?)",
            UUID.randomUUID().toString().replace("-",""),product,buyer,buyer,quantity,new BigDecimal(amount).subtract(new BigDecimal("1.25")),new BigDecimal(amount),state,created);
    }
    void review(long order,long product,long user,int rating,int status,String created) throws SQLException { insert("INSERT INTO product_review(order_id,product_id,buyer_id,seller_id,rating,status,create_time) VALUES(?,?,?,?,?,?,?)",order,product,user,user,rating,status,created); }
    long n(Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }

    @Test void actualMysqlQueriesMatchKnownFixturesAndRollback() throws Exception {
        var dataSource=new UnpooledDataSource("com.mysql.cj.jdbc.Driver","jdbc:mysql://127.0.0.1:3306/campus_system_dev_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai","campus_dev_test",System.getenv("CAMPUS_STATS_DB_PASSWORD"));
        var config=new Configuration(new Environment("statistics-test",new JdbcTransactionFactory(),dataSource));
        try(var xml=getClass().getClassLoader().getResourceAsStream("mapper/StatisticsMapper.xml")) { new XMLMapperBuilder(xml,config,"mapper/StatisticsMapper.xml",config.getSqlFragments()).parse(); }
        var factory=new SqlSessionFactoryBuilder().build(config);
        Map<String,Object> before;
        try(var session=factory.openSession(false)) {
            connection=session.getConnection(); connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            try(var statement=connection.createStatement();var r=statement.executeQuery("SELECT DATABASE(),CURRENT_USER()")) { assertTrue(r.next()); assertEquals("campus_system_dev_test",r.getString(1)); assertTrue(r.getString(2).startsWith("campus_dev_test@")); }
            var mapper=session.getMapper(StatisticsMapper.class); before=mapper.selectTotals();
            var start=LocalDateTime.of(2004,2,28,0,0); var end=LocalDateTime.of(2004,3,2,0,0);
            assertEquals(0,n(mapper.selectPeriod(start,end),"orders"),"fixture dates must be unused");
            try {
                long buyer=user("普通", "2004-02-28 00:00:00",true,false,1);
                user("禁用普通", "2004-03-01 23:59:59",true,false,2);
                user("双重身份", "2004-02-28 00:00:00",true,true,1);
                user("无用户身份", "2004-02-28 00:00:00",false,false,1);
                user("此前普通", "2004-02-27 23:59:59",true,false,1);
                user("边界之外", "2004-03-02 00:00:00",true,false,1);
                insert("INSERT INTO `user`(base_user_id) VALUES(?)",buyer); // duplicate role row cannot inflate count
                long category=insert("INSERT INTO category(name) VALUES('统计测试分类')");
                long product=product(buyer,category,"2004-02-28 00:00:00");
                product(buyer,-1,"2004-02-28 12:00:00");
                long first=0;
                for(int state=0;state<=5;state++) { long id=order(product,buyer,state,state==3?3:9,state==3?"31.25":"900.00","2004-02-28 00:00:00"); if(state==3)first=id; }
                long last=order(product,buyer,3,2,"20.05","2004-03-01 23:59:59");
                long missing=order(-1,buyer,3,4,"40.00","2004-03-01 12:00:00");
                order(product,buyer,99,9,"900.00","2004-02-28 00:00:00");
                order(product,buyer,3,99,"999.00","2004-02-27 23:59:59");
                order(product,buyer,3,99,"999.00","2004-03-02 00:00:00");
                for(int i=0;i<11;i++) {
                    long cat=insert("INSERT INTO category(name) VALUES(?)","统计测试分类"+i);
                    long prod=product(buyer,cat,"2004-02-28 12:00:00");
                    long ord=order(prod,buyer,3,1,"1.00","2004-02-28 12:00:00");
                    if(i==0)review(ord,prod,buyer,0,1,"2004-02-28 12:00:00");
                    if(i==1)review(ord,prod,buyer,5,1,"2004-03-02 00:00:00");
                }
                review(first,product,buyer,5,1,"2004-02-28 00:00:00");
                review(last,product,buyer,2,1,"2004-03-01 23:59:59");
                review(missing,product,buyer,5,0,"2004-02-28 12:00:00");
                session.clearCache();
                var period=mapper.selectPeriod(start,end);
                assertEquals(2,n(period,"users")); assertEquals(13,n(period,"products")); assertEquals(20,n(period,"orders"));
                assertEquals(14,n(period,"completedOrders")); assertEquals(20,n(period,"completedQuantity"));
                assertEquals(0,new BigDecimal("102.30").compareTo((BigDecimal)period.get("completedAmount")));
                // 与今日角标相同的单日查询：排除管理员，包含末日最后一秒，不含次日零点。
                var singleDay=mapper.selectPeriod(LocalDateTime.of(2004,3,1,0,0),end);
                assertEquals(1,n(singleDay,"users")); assertEquals(0,n(singleDay,"products")); assertEquals(2,n(singleDay,"orders"));
                var categories=mapper.selectCategories(start,end); assertEquals(13,categories.size(),"must not cut off categories after ten");
                assertEquals(13,categories.stream().mapToLong(r -> n(r,"products")).sum());
                assertEquals(14,categories.stream().mapToLong(r -> n(r,"completedOrders")).sum());
                assertEquals(20,categories.stream().mapToLong(r -> n(r,"completedQuantity")).sum());
                var uncategorized=categories.stream().filter(r -> n(r,"id")==0).findFirst().orElseThrow(); assertEquals(4,n(uncategorized,"completedQuantity"));
                var daily=mapper.selectDailyActivity(start,end); assertEquals(2,daily.size());
                assertEquals(2,daily.stream().mapToLong(r -> n(r,"users")).sum()); assertEquals(20,daily.stream().mapToLong(r -> n(r,"orders")).sum());
                var participation=mapper.selectParticipation(start,end); assertEquals(3,n(participation,"eligibleUsers")); assertEquals(1,n(participation,"buyers")); assertEquals(1,n(participation,"publishers"));
                var reviews=mapper.selectReviews(start,end); assertEquals(2,n(reviews,"visibleReviews")); assertEquals(1,n(reviews,"positiveReviews")); assertEquals(3,n(reviews,"reviewedCompletedOrders"));
                var statuses=mapper.selectOrderStatuses(start,end); assertEquals(20,statuses.stream().mapToLong(r -> n(r,"count")).sum()); assertTrue(statuses.stream().anyMatch(r -> n(r,"status")==99));
                assertEquals(n(before,"users")+4,n(mapper.selectTotals(),"users"));
                // 独立售后状态不改变原交易字段，但仪表盘优先归类为售后处理中，避免漏计或重复。
                long request=insert("INSERT INTO order_after_sale(order_id,request_type,state,reason,return_required,applicant_id) VALUES(?,'REGULAR','WAIT_ADMIN','统计售后验收',1,?)",first,buyer);
                session.clearCache();
                var pending=mapper.selectPeriod(start,end);assertEquals(13,n(pending,"completedOrders"));
                assertEquals(17,n(pending,"completedQuantity"));assertEquals(0,new BigDecimal("71.05").compareTo((BigDecimal)pending.get("completedAmount")));
                assertEquals(n(before,"refundOrders")+2,n(mapper.selectTotals(),"refundOrders"));
                assertEquals(2,mapper.selectOrderStatuses(start,end).stream().filter(r->n(r,"status")==5).mapToLong(r->n(r,"count")).sum());
                assertEquals(2,n(mapper.selectReviews(start,end),"reviewedCompletedOrders"));
                try(var change=connection.prepareStatement("UPDATE order_after_sale SET state='WITHDRAWN' WHERE id=?")){change.setLong(1,request);change.executeUpdate();}
                session.clearCache();assertEquals(14,n(mapper.selectPeriod(start,end),"completedOrders"));

            } finally { session.rollback(true); }
        }
        try(var session=factory.openSession()) { assertEquals(before,session.getMapper(StatisticsMapper.class).selectTotals(),"rollback must restore all business counts and amounts"); }
    }
}
