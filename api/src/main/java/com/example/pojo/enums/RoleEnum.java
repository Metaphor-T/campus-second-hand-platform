package com.example.pojo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色枚举类
 */
@Getter
@AllArgsConstructor
public enum RoleEnum {

    ADMIN("ADMIN"),
    USER("USER");

    private final String role; // 角色

}