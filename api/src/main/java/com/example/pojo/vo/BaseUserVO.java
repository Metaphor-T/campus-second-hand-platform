package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 基础用户信息VO
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseUserVO {
    private Integer id; // 主键ID
    private String account; // 账号
    private String avatar; // 头像
    private String username; // 用户名
    private Integer gender; // 性别（1：男；2-女）
    private String phone; // 手机号
    private Integer loginStatus; // 账号状态（1：正常；2-封号）
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime; // 创建时间
}
