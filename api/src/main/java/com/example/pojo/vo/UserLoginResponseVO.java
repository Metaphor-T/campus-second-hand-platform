package com.example.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应VO
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginResponseVO {
    private String token; // Token令牌
    private String role; // 角色
}
