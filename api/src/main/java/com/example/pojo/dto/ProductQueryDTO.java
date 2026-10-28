package com.example.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分页查询参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductQueryDTO extends QueryDTO {

    /** 用户ID（查询某用户发布的商品） */
    private Integer userId;

    /** 商品分类ID */
    private Integer categoryId;

    /** 商品标题（模糊搜索） */
    private String title;

    /** 审核状态筛选：0-待审核，1-审核通过，2-审核拒绝，3-已下架 */
    private Integer status;

    /** 新旧程度筛选 */
    private Integer condition;

    /** 交易方式筛选 */
    private Integer tradeType;

    /** 最低价格 */
    private java.math.BigDecimal minPrice;

    /** 最高价格 */
    private java.math.BigDecimal maxPrice;
}
