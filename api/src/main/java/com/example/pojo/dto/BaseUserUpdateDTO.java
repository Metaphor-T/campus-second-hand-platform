package com.example.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 基础信息修改参数接收DTO
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseUserUpdateDTO {
    @NotNull
    private Integer id; // 主键ID
    private String avatar; // 头像
    private String username; // 用户名
    private Integer gender; // 性别（1：男；2-女）
    private String phone; // 手机号
    private Integer loginStatus; // 账号状态（1：正常；2：封号）
}
