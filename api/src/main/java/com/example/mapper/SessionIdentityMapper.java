package com.example.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SessionIdentityMapper {
    record Identity(Integer id,Integer loginStatus,boolean userRole,boolean adminRole,String credentialDigest) {}
    @Select("""
        SELECT b.id,b.login_status AS loginStatus,
          EXISTS(SELECT 1 FROM `user` u WHERE u.base_user_id=b.id) AS userRole,
          EXISTS(SELECT 1 FROM admin a WHERE a.base_user_id=b.id) AS adminRole,
          SHA2(COALESCE(b.password,''),256) AS credentialDigest
        FROM base_user b WHERE b.id=#{id}
        """)
    Identity find(Integer id);

    @Select("SELECT b.id FROM base_user b JOIN admin a ON a.base_user_id=b.id ORDER BY b.id FOR UPDATE")
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    List<Integer> lockAdministrators();

    @Select("SELECT COUNT(*) FROM base_user b JOIN admin a ON a.base_user_id=b.id WHERE b.login_status=1")
    int activeAdministratorCount();
}
