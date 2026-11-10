package com.example.profile;

import com.example.mapper.UserProfileMapper;
import com.example.pojo.dto.UserProfileUpdateDTO;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="CAMPUS_STATS_DB_PASSWORD",matches=".+")
class UserProfileDatabaseTest {
    long insert(Connection c,String sql,Object...values)throws SQLException { try(var s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){for(int i=0;i<values.length;i++)s.setObject(i+1,values[i]);s.executeUpdate();try(var k=s.getGeneratedKeys()){return k.next()?k.getLong(1):0;}} }
    @Test void campusSaveClearOwnershipAndCascadeAreTransactional() throws Exception {
        var ds=new UnpooledDataSource("com.mysql.cj.jdbc.Driver","jdbc:mysql://127.0.0.1:3306/campus_system_dev_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai","campus_dev_test",System.getenv("CAMPUS_STATS_DB_PASSWORD"));
        var config=new Configuration(new Environment("profile-test",new JdbcTransactionFactory(),ds));config.addMapper(UserProfileMapper.class);var factory=new SqlSessionFactoryBuilder().build(config);
        long before;
        try(var session=factory.openSession(false)) {
            var c=session.getConnection();try(var s=c.createStatement();var r=s.executeQuery("SELECT DATABASE(),CURRENT_USER(),(SELECT COUNT(*) FROM user_campus_profile)")){assertTrue(r.next());assertEquals("campus_system_dev_test",r.getString(1));assertTrue(r.getString(2).startsWith("campus_dev_test@"));before=r.getLong(3);}
            try {
                int id=(int)insert(c,"INSERT INTO base_user(account,username,password,login_status,create_time) VALUES(?,'资料测试','Original123',1,'2004-01-01 12:34:56')","profile_"+UUID.randomUUID().toString().substring(0,10));
                insert(c,"INSERT INTO `user`(base_user_id) VALUES(?)",id);
                var mapper=session.getMapper(UserProfileMapper.class);assertTrue(mapper.hasUserRole(id));assertFalse(mapper.hasAdminRole(id));
                var initial=mapper.selectProfile(id);assertNull(initial.school());assertNull(initial.studentNumber());assertEquals("USER",initial.role());assertEquals("2004-01-01T12:34:56",initial.createTime().toString());
                var data=new UserProfileUpdateDTO();data.setUsername("保存后的昵称");data.setPhone("13912345678");data.setGender(2);data.setSchool("校园大学");data.setCollege("计算机学院");data.setStudentNumber("2026_0001");
                assertEquals(1,mapper.updateBasic(id,data,null));mapper.upsertCampus(id,data);
                var saved=mapper.selectProfile(id);assertEquals("校园大学",saved.school());assertEquals("计算机学院",saved.college());assertEquals("2026_0001",saved.studentNumber());assertEquals("保存后的昵称",saved.username());assertEquals(initial.account(),saved.account());
                data.setPhone(null);data.setSchool(null);data.setCollege(null);data.setStudentNumber(null);mapper.updateBasic(id,data,"/files/avatars/"+id+"/test.png");mapper.upsertCampus(id,data);
                var cleared=mapper.selectProfile(id);assertNull(cleared.phone());assertNull(cleared.studentNumber());assertTrue(cleared.avatar().contains("/avatars/"+id+"/"));
                assertEquals(0,mapper.changePassword(id,"wrong","NewPass123"));assertEquals(1,mapper.changePassword(id,"Original123","NewPass123"));
                insert(c,"UPDATE base_user SET login_status=2 WHERE id=?",id);assertEquals(0,mapper.updateBasic(id,data,null));
                insert(c,"DELETE FROM base_user WHERE id=?",id);
                try(var s=c.prepareStatement("SELECT COUNT(*) FROM user_campus_profile WHERE base_user_id=?")){s.setInt(1,id);try(var r=s.executeQuery()){assertTrue(r.next());assertEquals(0,r.getLong(1));}}
            } finally { session.rollback(true); }
        }
        try(var session=factory.openSession();var s=session.getConnection().createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM user_campus_profile")){assertTrue(r.next());assertEquals(before,r.getLong(1));}
    }
}
