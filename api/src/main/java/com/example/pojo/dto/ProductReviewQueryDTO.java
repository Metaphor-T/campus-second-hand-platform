package com.example.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品评价分页查询参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductReviewQueryDTO extends QueryDTO {

    /** 商品ID（查询某个商品的评价） */
    private Integer productId;

    /** 卖家用户ID（查询某个卖家收到的评价） */
    private Integer sellerId;

    /** 买家用户ID（查询某个买家写的评价） */
    private Integer buyerId;

    /** 评分筛选（1-5星） */
    private Integer rating;

    /** 状态筛选：1-显示，0-隐藏 */
    private Integer status;

    /** 是否有回复：true-有回复，false-无回复 */
    private Boolean hasReply;

    /** 关键词模糊搜索（匹配评价内容） */
    private String keyword;
}
