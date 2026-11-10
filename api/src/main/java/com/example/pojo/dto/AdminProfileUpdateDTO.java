package com.example.pojo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminProfileUpdateDTO {
    @NotBlank(message="昵称不能为空") @Size(max=20,message="昵称最多20个字符")
    private String username;
    @Pattern(regexp="^$|^1[0-9]{10}$",message="手机号请填写11位数字，以1开头，或留空")
    private String phone;
    @Min(1) @Max(2) private Integer gender;
}
