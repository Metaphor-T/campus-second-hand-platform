package com.example.profile;

import com.example.campus.CampusVerificationRecord;
import com.example.mapper.*;
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
class CampusVerificationDatabaseTest {
    @Test void snapshotsInvalidationGuardedCompletionHistoryAndCascadeWorkInRealMysql() throws Exception {
        var ds=new UnpooledDataSource("com.mysql.cj.jdbc.Driver","jdbc:mysql://127.0.0.1:3306/campus_system_dev_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai","campus_dev_test",System.getenv("CAMPUS_STATS_DB_PASSWORD"));
        var config=new Configuration(new Environment("verification-test",new JdbcTransactionFactory(),ds));config.setMapUnderscoreToCamelCase(true);config.addMapper(CampusVerificationMapper.class);config.addMapper(UserProfileMapper.class);
        var factory=new SqlSessionFactoryBuilder().build(config);
        try(var session=factory.openSession(false)) {
            var c=session.getConnection();var helper=new UserProfileDatabaseTest();
            try(var st=c.createStatement();var r=st.executeQuery("SELECT DATABASE(),CURRENT_USER()")) { assertTrue(r.next());assertEquals("campus_system_dev_test",r.getString(1));assertTrue(r.getString(2).startsWith("campus_dev_test@")); }
            try {
                int id=(int)helper.insert(c,"INSERT INTO base_user(account,username,password,login_status) VALUES(?,'认证测试','Test123!',1)","verify_"+UUID.randomUUID().toString().substring(0,12));
                var mapper=session.getMapper(CampusVerificationMapper.class);
                assertNull(mapper.latest(id));assertEquals(id,session.getMapper(UserProfileMapper.class).lockActiveUser(id));
                var record=new CampusVerificationRecord();record.setBaseUserId(id);record.setRequestId(UUID.randomUUID().toString());record.setSchool("示例大学");record.setStudentNumber("TEST_2026");record.setProvider("unit_test_only");
                assertEquals(1,mapper.insert(record));assertNotNull(record.getId());
                var pending=mapper.latest(id);assertEquals("PENDING",pending.getStatus());assertEquals("示例大学",pending.getSchool());assertNotNull(pending.getRequestedAt());
                assertEquals(0,mapper.invalidateChanged(id,"示例大学","TEST_2026"));
                // 大小写变化也失效；恢复旧字段不能恢复原认证。
                assertEquals(1,mapper.invalidateChanged(id,"示例大学","test_2026"));assertEquals("INVALIDATED",mapper.latest(id).getStatus());
                pending.setStatus("VERIFIED");assertEquals(0,mapper.complete(pending));assertEquals(0,mapper.invalidateChanged(id,"示例大学","TEST_2026"));
                record.setId(null);record.setRequestId(UUID.randomUUID().toString());mapper.insert(record);
                var next=mapper.latest(id);assertEquals("PENDING",next.getStatus());assertNotEquals(pending.getId(),next.getId());
                next.setBaseUserId(id+1);next.setStatus("REJECTED");assertEquals(0,mapper.complete(next));next.setBaseUserId(id);assertEquals(1,mapper.complete(next));assertEquals(0,mapper.complete(next));
                assertEquals("REJECTED",mapper.latest(id).getStatus());
                try(var st=c.prepareStatement("SELECT COUNT(*) FROM campus_verification WHERE base_user_id=?")){st.setInt(1,id);try(var r=st.executeQuery()){assertTrue(r.next());assertEquals(2,r.getInt(1));}}
                assertEquals(1,mapper.invalidateChanged(id,null,null));
                helper.insert(c,"DELETE FROM base_user WHERE id=?",id);assertNull(mapper.latest(id));
            } finally { session.rollback(true); }
        }
    }
}
