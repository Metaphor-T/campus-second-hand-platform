package com.example.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录参数接受类
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginDTO {

    @NotBlank(message = "账号不能为空")
    @Size(min = 3,max = 30,message = "账号长度在3-30之间")
    private String account; // 账号
    @NotBlank(message = "密码不能为空")
    private String password; // 密码

}
