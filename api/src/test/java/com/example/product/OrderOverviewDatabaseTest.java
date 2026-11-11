package com.example.product;

import com.example.mapper.OrderMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="CAMPUS_STATS_DB_PASSWORD",matches=".+")
class OrderOverviewDatabaseTest {
    private int insert(Connection c,String sql,Object... values)throws SQLException {
        try(var s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for(int i=0;i<values.length;i++)s.setObject(i+1,values[i]);
            s.executeUpdate();try(var r=s.getGeneratedKeys()){return r.next()?r.getInt(1):0;}
        }
    }
    @Test void actualMapperProtectsAddressOwnershipAndKeepsHistoricalOrders()throws Exception {
        var ds=new UnpooledDataSource("com.mysql.cj.jdbc.Driver","jdbc:mysql://127.0.0.1:3306/campus_system_dev_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai","campus_dev_test",System.getenv("CAMPUS_STATS_DB_PASSWORD"));
        var config=new Configuration(new Environment("order-test",new JdbcTransactionFactory(),ds));
        try(var xml=Resources.getResourceAsStream("mapper/OrderMapper.xml")){new XMLMapperBuilder(xml,config,"mapper/OrderMapper.xml",config.getSqlFragments()).parse();}
        var factory=new SqlSessionFactoryBuilder().build(config);
        String no="QA"+UUID.randomUUID().toString().replace("-","").substring(0,25);
        try(var session=factory.openSession(false)) {
            var c=session.getConnection();
            try(var s=c.createStatement();var r=s.executeQuery("SELECT DATABASE(),CURRENT_USER()")){assertTrue(r.next());assertEquals("campus_system_dev_test",r.getString(1));assertTrue(r.getString(2).startsWith("campus_dev_test@"));}
            try {
                int product,trade;
                try(var s=c.createStatement();var r=s.executeQuery("SELECT id,trade_type FROM product ORDER BY id LIMIT 1")){assertTrue(r.next());product=r.getInt(1);trade=r.getInt(2);}
                String addressSql="INSERT INTO address(user_id,receiver_name,receiver_phone,province,city,district,detail_address) VALUES(?,'验收收货人','13900000000','测试省','测试市','测试区','测试地址')";
                int owned=insert(c,addressSql,2),foreign=insert(c,addressSql,3);
                int id=insert(c,"INSERT INTO `order`(order_no,product_id,buyer_id,seller_id,address_id,quantity,unit_price,total_amount,shipping_fee,pay_amount,order_status,create_time) VALUES(?,?,2,3,?,1,9.90,9.90,0,9.90,0,'2026-11-01 12:34:56')",no,product,owned);
                var mapper=session.getMapper(OrderMapper.class);var order=mapper.getDetail(id);
                assertEquals(trade,order.getProductTradeType());assertEquals("9.90",order.getPayAmount().toPlainString());assertEquals("2026-11-01T12:34:56",order.getCreateTime().toString());assertEquals("验收收货人",order.getReceiverName());assertTrue(order.getFullAddress().contains("测试地址"));
                insert(c,"UPDATE `order` SET address_id=? WHERE id=?",foreign,id);session.clearCache();order=mapper.getDetail(id);
                assertNotNull(order);assertNull(order.getReceiverName());assertNull(order.getReceiverPhone());assertTrue(order.getFullAddress()==null||order.getFullAddress().isBlank());
                insert(c,"UPDATE `order` SET product_id=2147483647,address_id=NULL WHERE id=?",id);session.clearCache();order=mapper.getDetail(id);
                assertNotNull(order);assertNull(order.getProductTitle());assertEquals(no,order.getOrderNo());assertEquals("9.90",order.getPayAmount().toPlainString());
            } finally {session.rollback(true);}
        }
        try(var session=factory.openSession();var s=session.getConnection().prepareStatement("SELECT COUNT(*) FROM `order` WHERE order_no=?")){s.setString(1,no);try(var r=s.executeQuery()){assertTrue(r.next());assertEquals(0,r.getInt(1));}}
    }
}
