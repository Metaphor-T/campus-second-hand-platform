package com.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "admin")
public class Admin {
  
    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID
    private Integer baseUserId; // 基础用户信息ID

}
