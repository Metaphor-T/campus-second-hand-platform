package com.example.profile;

import com.example.mapper.SessionIdentityMapper;
import com.example.utils.JwtUtil;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="CAMPUS_STATS_DB_PASSWORD",matches=".+")
class IdentityDatabaseTest {
    @Test void currentIdentityRolesStatusAndPasswordBindingAreReadFromRealTestDatabase() throws Exception {
        var ds=new UnpooledDataSource("com.mysql.cj.jdbc.Driver","jdbc:mysql://127.0.0.1:3306/campus_system_dev_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai","campus_dev_test",System.getenv("CAMPUS_STATS_DB_PASSWORD"));
        var config=new Configuration(new Environment("identity-test",new JdbcTransactionFactory(),ds));config.setMapUnderscoreToCamelCase(true);config.addMapper(SessionIdentityMapper.class);
        try(var session=new SqlSessionFactoryBuilder().build(config).openSession(false)) {
            var c=session.getConnection();var helper=new UserProfileDatabaseTest();
            try(var st=c.createStatement();var r=st.executeQuery("SELECT DATABASE(),CURRENT_USER()")) { assertTrue(r.next());assertEquals("campus_system_dev_test",r.getString(1));assertTrue(r.getString(2).startsWith("campus_dev_test@")); }
            try {
                var mapper=session.getMapper(SessionIdentityMapper.class);int original=mapper.activeAdministratorCount();
                String password="测试Password!2026";
                int id=(int)helper.insert(c,"INSERT INTO base_user(account,username,password,login_status) VALUES(?,'身份测试',?,1)","identity_"+UUID.randomUUID().toString().substring(0,10),password);
                helper.insert(c,"INSERT INTO `user`(base_user_id) VALUES(?)",id);session.clearCache();
                var ordinary=mapper.find(id);assertTrue(ordinary.userRole());assertFalse(ordinary.adminRole());assertEquals(1,ordinary.loginStatus());assertEquals(JwtUtil.passwordDigest(password),ordinary.credentialDigest());
                helper.insert(c,"DELETE FROM `user` WHERE base_user_id=?",id);helper.insert(c,"INSERT INTO admin(base_user_id) VALUES(?)",id);session.clearCache();
                assertFalse(mapper.find(id).userRole());assertTrue(mapper.find(id).adminRole());assertTrue(mapper.lockAdministrators().contains(id));assertEquals(original+1,mapper.activeAdministratorCount());
                helper.insert(c,"UPDATE base_user SET login_status=2,password='Changed123' WHERE id=?",id);session.clearCache();
                var banned=mapper.find(id);assertEquals(2,banned.loginStatus());assertEquals(JwtUtil.passwordDigest("Changed123"),banned.credentialDigest());assertEquals(original,mapper.activeAdministratorCount());
                helper.insert(c,"DELETE FROM admin WHERE base_user_id=?",id);session.clearCache();assertFalse(mapper.find(id).adminRole());
                helper.insert(c,"DELETE FROM base_user WHERE id=?",id);session.clearCache();assertNull(mapper.find(id));
            } finally { session.rollback(true); }
        }
    }
}
