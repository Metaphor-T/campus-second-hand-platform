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
 * 商品评价实体 - 对应数据库表 product_review
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "product_review")
public class ProductReview {

    /** 评价主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 订单ID（关联 order.id，一个订单只能评价一次） */
    private Integer orderId;

    /** 商品ID（关联 product.id） */
    private Integer productId;

    /** 买家用户ID（关联 base_user.id） */
    private Integer buyerId;

    /** 卖家用户ID（关联 base_user.id） */
    private Integer sellerId;

    /** 评分：1-5星 */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 卖家回复内容 */
    private String replyContent;

    /** 卖家回复时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime replyTime;

    /** 状态：1-显示，0-隐藏（管理员隐藏违规评价） */
    private Integer status;

    /** 评价时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
