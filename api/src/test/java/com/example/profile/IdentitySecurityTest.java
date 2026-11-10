package com.example.profile;

import com.example.mapper.SessionIdentityMapper;
import com.example.service.SessionIdentityService;
import com.example.exception.BusinessException;
import com.example.utils.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdentitySecurityTest {
    SessionIdentityMapper mapper;SessionIdentityService service;JwtUtil jwt;
    static JwtUtil jwt(byte value) {
        var jwt=new JwtUtil();var key=new byte[64];java.util.Arrays.fill(key,value);
        ReflectionTestUtils.setField(jwt,"secret",Base64.getEncoder().encodeToString(key));jwt.initialize();return jwt;
    }
    SessionIdentityMapper.Identity row(int status,boolean user,boolean admin,String password) {
        return new SessionIdentityMapper.Identity(2,status,user,admin,JwtUtil.passwordDigest(password));
    }
    String proof(String token) { return jwt.getClaims(token).get("credential",String.class); }
    void denied(Runnable action) { assertEquals(401,assertThrows(BusinessException.class,action::run).getCode()); }
    @BeforeEach void setup() { mapper=mock(SessionIdentityMapper.class);jwt=jwt((byte)21);service=new SessionIdentityService(mapper,jwt);when(mapper.find(2)).thenReturn(row(1,true,false,"Original123")); }
    @AfterEach void cleanup() { UserContext.clear(); }
    @Test void activeUserWithCurrentCredentialCanAuthenticate() { String token=jwt.generateToken(2,"USER","Original123");service.validateSession(2,"USER",proof(token)); }
    @Test void deletedAccountCannotReuseToken() { when(mapper.find(2)).thenReturn(null);denied(()->service.validateSession(2,"USER","old")); }
    @Test void disabledAccountCannotReuseToken() { when(mapper.find(2)).thenReturn(row(2,true,false,"Original123"));denied(()->service.validateSession(2,"USER","old")); }
    @Test void removedRoleCannotReuseToken() { when(mapper.find(2)).thenReturn(row(1,false,false,"Original123"));denied(()->service.validateSession(2,"USER","old")); }
    @Test void ordinaryAccountCannotBecomeAdminThroughRoleClaim() { String token=jwt.generateToken(2,"ADMIN","Original123");denied(()->service.validateSession(2,"ADMIN",proof(token))); }
    @Test void unknownRoleAndMissingIdentityAreRejected() { denied(()->service.validateSession(2,"SUPERADMIN","x"));denied(()->service.validateSession(null,"USER","x")); }
    @Test void passwordChangeInvalidatesExistingTokenAcrossSessions() { String token=jwt.generateToken(2,"USER","Original123");when(mapper.find(2)).thenReturn(row(1,true,false,"NewPass123"));denied(()->service.validateSession(2,"USER",proof(token))); }
    @Test void newPasswordCreatesValidFreshSession() { when(mapper.find(2)).thenReturn(row(1,true,false,"NewPass123"));String token=jwt.generateToken(2,"USER","NewPass123");service.validateSession(2,"USER",proof(token)); }
    @Test void tokenWithoutCredentialBindingRequiresNewLogin() { denied(()->service.validateSession(2,"USER",null)); }
    @Test void credentialCannotBeReusedForOtherUserOrRole() { String p=proof(jwt.generateToken(2,"USER","Original123"));assertFalse(jwt.matchesCredential(p,3,"USER",JwtUtil.passwordDigest("Original123")));assertFalse(jwt.matchesCredential(p,2,"ADMIN",JwtUtil.passwordDigest("Original123"))); }
    @Test void differentKeyCannotSignValidToken() { assertFalse(jwt.validateToken(jwt((byte)22).generateToken(2,"USER","Original123"))); }
    @Test void changedTokenPayloadFailsSignature() { String token=jwt.generateToken(2,"USER","Original123");String[] parts=token.split("\\.");parts[1]=Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"1\",\"role\":\"ADMIN\"}".getBytes());assertFalse(jwt.validateToken(String.join(".",parts))); }
    @Test void tokenDoesNotRevealPasswordOrItsBareDigest() { var claims=jwt.getClaims(jwt.generateToken(2,"USER","Original123"));assertFalse(claims.toString().contains("Original123"));assertFalse(claims.toString().contains(JwtUtil.passwordDigest("Original123"))); }
    @Test void weakKeyFailsStartup() { var weak=new JwtUtil();ReflectionTestUtils.setField(weak,"secret",Base64.getEncoder().encodeToString(new byte[16]));assertThrows(IllegalStateException.class,weak::initialize); }
    @Test void serviceRoleCheckRejectsMissingOrWrongIdentity() { assertEquals(403,assertThrows(BusinessException.class,()->service.require("ADMIN")).getCode());UserContext.setUser(2,"USER");assertEquals(403,assertThrows(BusinessException.class,()->service.require("ADMIN")).getCode()); }
}
