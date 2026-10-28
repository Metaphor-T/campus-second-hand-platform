package com.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商品想要记录实体 - 对应数据库表 product_want
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "product_want")
public class ProductWant {

    /** 想要记录主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 用户ID（关联 user.id） */
    private Integer userId;

    /** 商品ID（关联 product.id） */
    private Integer productId;

    /** 点击想要的时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
