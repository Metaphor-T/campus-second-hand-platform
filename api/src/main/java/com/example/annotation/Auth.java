package com.example.annotation;

import com.example.pojo.enums.RoleEnum;

import java.lang.annotation.*;

/**
 * 权限注解 - 需要登录并拥有指定角色
 * 使用示例：
 * - @Auth                    // 只需登录
 * - @Auth("ADMIN")           // 需要 ADMIN 角色
 * - @Auth({"ADMIN", "USER"}) // 需要 ADMIN 或 USER 角色（满足其一即可）
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auth {

    /**
     * 允许的角色列表，为空时只需要登录
     */
    RoleEnum[] value() default {};

}