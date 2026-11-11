package com.example.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import com.example.security.JwtKeyProvider;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

@Component
public class JwtUtil {
    @Value("${security.jwt.secret:}") private String secret;
    @Autowired private JwtKeyProvider keyProvider;
    private Key key;
    private byte[] secretBytes;
    private static final long EXPIRATION=7L*24*60*60*1000;
    @PostConstruct public void initialize() {
        secretBytes=keyProvider==null?JwtKeyProvider.decode(secret):keyProvider.resolve(secret);
        key=Keys.hmacShaKeyFor(secretBytes);
    }
    public String generateToken(Integer id,String role,String password) {
        return Jwts.builder().setSubject(String.valueOf(id)).setIssuer("campus-system")
            .claim("role",role).claim("credential",credential(id,role,passwordDigest(password)))
            .setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis()+EXPIRATION))
            .signWith(key,SignatureAlgorithm.HS256).compact();
    }
    public static String passwordDigest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(Exception e) { throw new IllegalStateException("凭据校验不可用"); }
    }
    private String credential(Integer id,String role,String digest) {
        try {
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secretBytes,"HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(("campus-credential:"+id+":"+role+":"+digest).getBytes(StandardCharsets.UTF_8)));
        } catch(Exception e) { throw new IllegalStateException("登录凭据校验不可用"); }
    }
    public boolean matchesCredential(String actual,Integer id,String role,String digest) {
        if(actual==null || digest==null) return false;
        return MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),credential(id,role,digest).getBytes(StandardCharsets.UTF_8));
    }
    public Claims getClaims(String token) { return Jwts.parserBuilder().setSigningKey(key).requireIssuer("campus-system").build().parseClaimsJws(token).getBody(); }
    public boolean validateToken(String token) { try { getClaims(token);return true; } catch(Exception e) { return false; } }
    public String getUserId(String token) { return getClaims(token).getSubject(); }
    public String getRole(String token) { return getClaims(token).get("role",String.class); }
}
