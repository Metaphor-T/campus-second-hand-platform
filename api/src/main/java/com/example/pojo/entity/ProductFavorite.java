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
 * 商品收藏实体 - 对应数据库表 product_favorite
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "product_favorite")
public class ProductFavorite {

    /** 收藏主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 用户ID（关联 user.id） */
    private Integer userId;

    /** 商品ID（关联 product.id） */
    private Integer productId;

    /** 收藏时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
