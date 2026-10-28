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
 * 评价图片实体 - 对应数据库表 review_image
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "review_image")
public class ReviewImage {

    /** 图片主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 评价ID（关联 product_review.id） */
    private Integer reviewId;

    /** 图片URL地址 */
    private String imageUrl;

    /** 排序权重 */
    private Integer sort;

    /** 上传时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
