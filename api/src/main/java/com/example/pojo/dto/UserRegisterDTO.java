package com.example.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册参数接受类
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterDTO {
    @NotBlank(message = "账号不能为空")
    @Size(min = 3,max = 30,message = "账号长度在3-30之间")
    private String account; // 账号
    @NotBlank(message = "密码不能为空")
    private String password; // 密码
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1,max = 20,message = "用户长度在1-20之间")
    private String username; // 用户名
}
