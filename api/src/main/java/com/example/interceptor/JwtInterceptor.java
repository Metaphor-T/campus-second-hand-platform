package com.example.interceptor;

import com.example.annotation.Auth;
import com.example.exception.BusinessException;
import com.example.pojo.api.Result;
import com.example.pojo.api.ResultCode;
import com.example.pojo.enums.RoleEnum;
import com.example.utils.JwtUtil;
import com.example.utils.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * Jwt拦截器
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    // 不需要拦截的路径（白名单）
    private static final String[] EXCLUDE_PATHS = {
            "/login",
            "/register",
            "/files/upload",
            "/ws/chat",
            "/guest"  // 游客端 API 全部免登录
    };

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String path = request.getRequestURI();

        // 检查是否在白名单中（白名单路径直接放行，不需要任何验证）
        if (isExcludePath(path)) {
            return true;
        }

        // 如果不是方法级别的映射，直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 获取权限注解（优先方法级，其次类级）
        Auth auth = getAuthAnnotation(method);

        // ========== 修复：没有 @Auth 注解，默认需要登录 ==========
        // 如果没有 @Auth 注解，说明接口需要登录（默认行为）
        // 只有白名单路径和显式标记的公开接口才能免登录

        // 从 Header 中获取 token
        String token = request.getHeader("Authorization");

        if (token == null || token.trim().isEmpty()) {
            handleUnauthorized(response, "未提供认证令牌");
            return false;
        }

        // 去除 Bearer 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            // 验证 token
            Claims claims = jwtUtil.getClaims(token);
            if (claims == null) {
                throw new BusinessException("请先登录...");
            }

            // 解析用户信息
            String userIdStr = jwtUtil.getUserId(token);
            if (userIdStr == null || userIdStr.isEmpty()) {
                throw new BusinessException("token中无用户信息");
            }
            String role = jwtUtil.getRole(token);
            int userId = Integer.parseInt(userIdStr);

            // 将用户信息存储到 ThreadLocal
            UserContext.setUser(userId, role);

            // ========== 角色权限校验 ==========
            if (auth != null) {
                RoleEnum[] requiredRoles = auth.value();

                // 如果注解中指定了角色，则需要校验角色
                if (requiredRoles.length > 0) {
                    String currentRole = UserContext.getRole();
                    if (!hasRequiredRole(currentRole, requiredRoles)) {
                        handleForbidden(response, "权限不足，不可操作");
                        return false;
                    }
                }
                // 有 @Auth 注解但没有指定角色，只需要登录即可，已经登录成功，放行
            }
            // 如果没有 @Auth 注解，只需要登录即可，已经登录成功，放行

            return true;
        } catch (Exception e) {
            handleUnauthorized(response, "认证失败: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }

    /**
     * 获取方法或类上的 @Auth 注解
     */
    private Auth getAuthAnnotation(Method method) {
        // 优先检查方法级别
        if (method.isAnnotationPresent(Auth.class)) {
            return method.getAnnotation(Auth.class);
        }
        // 检查类级别
        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass.isAnnotationPresent(Auth.class)) {
            return declaringClass.getAnnotation(Auth.class);
        }
        return null;
    }

    /**
     * 检查当前用户是否拥有所需角色（支持枚举和字符串匹配）
     */
    private boolean hasRequiredRole(String currentRole, RoleEnum[] requiredRoles) {
        if (currentRole == null) {
            return false;
        }

        // 匹配枚举名称或枚举中的role值
        for (RoleEnum requiredRole : requiredRoles) {
            if (requiredRole.name().equals(currentRole) ||
                    requiredRole.getRole().equals(currentRole)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理未授权响应
     */
    private void handleUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<String> fail = Result.fail(ResultCode.UNAUTHORIZED, message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(fail));
    }

    /**
     * 处理无权限响应
     */
    private void handleForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        Result<String> fail = Result.fail(ResultCode.FORBIDDEN, message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(fail));
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isExcludePath(String path) {
        for (String excludePath : EXCLUDE_PATHS) {
            if (path.contains(excludePath)) {
                return true;
            }
        }
        return false;
    }
}