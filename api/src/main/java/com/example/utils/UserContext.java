package com.example.utils;

/**
 * 用户上下文工具类
 */
public class UserContext {

    // 用户信息 record（JDK14+新特性）
    public record UserInfo(Integer userId, String role) {
    }

    // 线程局部变量
    private static final ThreadLocal<UserInfo> CONTEXT = new ThreadLocal<>();

    // 设置用户信息
    public static void setUser(Integer userId, String role) {
        CONTEXT.set(new UserInfo(userId, role));
    }

    // 获取用户信息
    public static UserInfo getUser() {
        return CONTEXT.get();
    }

    // 获取用户ID
    public static Integer getUserId() {
        UserInfo userInfo = CONTEXT.get();
        return userInfo != null ? userInfo.userId() : null;
    }

    // 获取角色
    public static String getRole() {
        UserInfo userInfo = CONTEXT.get();
        return userInfo != null ? userInfo.role() : null;
    }

    // 检查是否已登录
    public static boolean isLoggedIn() {
        return CONTEXT.get() != null;
    }

    // 清除
    public static void clear() {
        CONTEXT.remove();
    }
}