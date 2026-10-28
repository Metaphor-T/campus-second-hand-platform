package com.example.pojo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账号状态枚举类
 */
@Getter
@AllArgsConstructor
public enum AccountStatusEnum {

    NORMAL(1, "正常"),
    FORBIDDEN_LOGIN(2, "封号");

    private final Integer statusCode; // 状态码
    private final String statusText; // 状态文本

}
