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
 * 商品图册实体 - 对应数据库表 product_image
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "product_image")
public class ProductImage {

    /** 图片主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 商品ID（关联 product.id） */
    private Integer productId;

    /** 图片URL地址 */
    private String imageUrl;

    /** 是否为封面图：1-是，0-否 */
    private Integer isCover;

    /** 排序权重 */
    private Integer sort;

    /** 上传时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
