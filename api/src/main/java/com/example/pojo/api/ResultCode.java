package com.example.pojo.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举类
 **/
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    BAD_REQUEST(400, "参数错误");

    private final Integer code; // 响应码
    private final String message; // 响应码描述

}
