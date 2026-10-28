package com.example.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码参数接收类
 **/
@Data
public class UserUpdatePasswordDTO {
    @NotBlank(message = "新密码不能为空")
    @Size(min = 4,max = 64,message = "密码长度在4至64位")
    private String password; // 密码
}
