package com.example.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 基础用户信息查询条件类
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class BaseUserQueryDTO extends QueryDTO {
    private String account; // 账号
    private String username; // 用户名
    private Integer gender; // 性别（1：男；2-女）
    private String phone; // 手机号
    private Integer loginStatus; // 账号状态（1：正常；2：封号）
}
