package com.example.pojo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserProfilePasswordDTO {
    @NotBlank(message="请输入原密码") @Size(max=64,message="原密码长度无效")
    private String oldPassword;
    @NotBlank(message="请输入新密码") @Size(min=8,max=64,message="新密码长度为8至64位")
    private String newPassword;
    @NotBlank(message="请再次输入新密码") @Size(max=64,message="确认密码长度无效")
    private String confirmPassword;
}
