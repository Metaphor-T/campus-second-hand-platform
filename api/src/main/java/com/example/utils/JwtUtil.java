package com.example.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Jwt工具类
 */
@Component
public class JwtUtil {

    // 密钥
    private static final String SECRET_KEY = "yF3jK9mQ2wR8tL5nX7pB1vC4xZ6aD9gH2jK5mN8pQ3rT6uW9y";

    // 过期时间（7天）
    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000;

    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // 生成 token（带角色）
    public String generateToken(Integer userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 验证 token
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 获取用户ID
    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    // 获取角色
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // 解析 token
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}