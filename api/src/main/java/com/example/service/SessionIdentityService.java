package com.example.service;

import com.example.exception.BusinessException;
import com.example.mapper.SessionIdentityMapper;
import com.example.utils.UserContext;
import com.example.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class SessionIdentityService {
    private final SessionIdentityMapper identities;
    private final JwtUtil jwt;
    private SessionIdentityMapper.Identity current(Integer id,String role) {
        var identity=id==null?null:identities.find(id);
        boolean valid=identity!=null && Integer.valueOf(1).equals(identity.loginStatus())
            && ("USER".equals(role)?identity.userRole():"ADMIN".equals(role)&&identity.adminRole());
        if(!valid) throw new BusinessException("账号已失效、被禁用或身份权限已变化，请重新登录",401);
        return identity;
    }
    public void validateSession(Integer id,String role,String credential) {
        var identity=current(id,role);
        if(!jwt.matchesCredential(credential,id,role,identity.credentialDigest()))
            throw new BusinessException("登录状态已变化，请重新登录",401);
    }
    public Integer require(String role) {
        if(!role.equals(UserContext.getRole())) throw new BusinessException("权限不足",403);
        Integer id=UserContext.getUserId();current(id,role);return id;
    }
}
