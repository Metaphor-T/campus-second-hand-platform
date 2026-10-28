package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品视图对象 - 返回给前端的数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVO {

    /** 商品主键ID */
    private Integer id;

    /** 发布用户ID */
    private Integer userId;

    /** 发布用户名称 */
    private String username;

    /** 发布用户头像 */
    private String userAvatar;

    /** 商品分类ID */
    private Integer categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 商品标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 商品价格 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 新旧程度：1-全新，2-几乎全新，3-有使用痕迹，4-老旧 */
    private Integer condition;

    /** 交易方式：1-自提，2-邮寄，3-两者皆可 */
    private Integer tradeType;

    /** 审核状态：0-待审核，1-审核通过，2-审核拒绝，3-已下架 */
    private Integer status;

    /** 浏览量 */
    private Integer viewCount;

    /** 想要数 */
    private Integer wantCount;

    /** 收藏数 */
    private Integer favoriteCount;

    /** 评价数 */
    private Integer reviewCount;

    /** 平均评分 */
    private BigDecimal rating;

    /** 库存数量（0表示不限库存） */
    private Integer stock;

    /** 乐观锁版本号 */
    private Integer version;

    /** 审核备注 */
    private String auditRemark;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditTime;

    /** 审核管理员ID */
    private Integer auditorId;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /** 商品图册列表 */
    private List<ProductImageVO> images;
}

