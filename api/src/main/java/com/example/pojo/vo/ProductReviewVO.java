package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品评价视图对象 - 返回给前端的数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductReviewVO {

    /** 评价主键ID */
    private Integer id;

    /** 订单ID */
    private Integer orderId;

    /** 商品ID */
    private Integer productId;

    /** 商品标题 */
    private String productTitle;

    /** 商品首图 */
    private String productImage;

    /** 买家用户ID */
    private Integer buyerId;

    /** 买家用户名 */
    private String buyerName;

    /** 买家头像 */
    private String buyerAvatar;

    /** 卖家用户ID */
    private Integer sellerId;

    /** 卖家用户名 */
    private String sellerName;

    /** 评分：1-5星 */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 卖家回复内容 */
    private String replyContent;

    /** 卖家回复时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime replyTime;

    /** 状态：1-显示，0-隐藏 */
    private Integer status;

    /** 评价图片列表 */
    private List<String> images;

    /** 评价时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
