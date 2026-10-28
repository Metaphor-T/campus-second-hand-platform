package com.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体 - 对应数据库表 product（含评价统计冗余字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "product")
public class Product {

    /** 商品主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 发布用户ID（关联 base_user.id） */
    private Integer userId;

    /** 商品分类ID（关联 category.id） */
    private Integer categoryId;

    /** 商品标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 商品价格 */
    private BigDecimal price;

    /** 原价（用于显示折扣） */
    private BigDecimal originalPrice;

    /** 新旧程度：1-全新，2-几乎全新，3-有使用痕迹，4-老旧 */
    @TableField(value = "`condition`")
    private Integer condition;

    /** 交易方式：1-自提，2-邮寄，3-两者皆可 */
    private Integer tradeType;

    /** 审核状态：0-待审核，1-审核通过，2-审核拒绝，3-已下架 */
    private Integer status;

    /** 浏览量 */
    private Integer viewCount;

    /** 想要数（冗余字段） */
    private Integer wantCount;

    /** 收藏数（冗余字段） */
    private Integer favoriteCount;

    /** 评价数（冗余字段） */
    private Integer reviewCount;

    /** 平均评分（冗余字段，保留一位小数） */
    private java.math.BigDecimal rating;

    /** 库存数量（0表示不限库存） */
    private Integer stock;

    /** 乐观锁版本号（防止超卖） */
    @TableField(value = "`version`")
    private Integer version;

    /** 审核备注（拒绝原因等） */
    private String auditRemark;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditTime;

    /** 审核管理员ID（关联 admin.id） */
    private Integer auditorId;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
