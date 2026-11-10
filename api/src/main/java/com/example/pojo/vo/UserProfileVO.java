package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/** 仅本人/管理员的资料接口使用，不能复用到公开卖家资料或普通用户列表。 */
public record UserProfileVO(Integer id,String account,String username,String avatar,Integer gender,String phone,
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8") LocalDateTime createTime,
    String school,String college,String studentNumber,String role) {}
