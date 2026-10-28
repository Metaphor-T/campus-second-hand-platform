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
 * 商品浏览记录实体类
 * <p>映射 product_view 表，记录每次商品详情页的浏览行为</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "product_view")
public class ProductView {

    /** 浏览记录主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 用户ID（关联 user.id，可为 null 表示未登录浏览） */
    private Integer userId;

    /** 商品ID（关联 product.id） */
    private Integer productId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
