package com.example.pojo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** 身份取自登录上下文；不接收账号、目标用户 ID、角色或封禁状态。空字符串表示清空选填资料。 */
@Data
public class UserProfileUpdateDTO {
    @NotBlank(message="昵称不能为空") @Size(max=20,message="昵称最多20个字符")
    private String username;
    @Pattern(regexp="^$|^1[0-9]{10}$",message="手机号请填写11位数字，以1开头，或留空")
    private String phone;
    @Min(value=1,message="性别选项无效") @Max(value=2,message="性别选项无效")
    private Integer gender;
    @Size(max=100,message="学校名称最多100个字符") @Pattern(regexp="[^\\p{Cntrl}]*",message="学校名称不能包含控制字符")
    private String school;
    @Size(max=100,message="学院名称最多100个字符") @Pattern(regexp="[^\\p{Cntrl}]*",message="学院名称不能包含控制字符")
    private String college;
    @Pattern(regexp="^$|^[A-Za-z0-9_-]{1,32}$",message="学号最多32位，仅支持字母、数字、横线和下划线")
    private String studentNumber;
}
