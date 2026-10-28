package com.example.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 修改商品参数（仅更新非 null 字段）
 */
@Data
public class ProductUpdateDTO {

    /** 商品主键ID */
    @NotNull(message = "商品ID不能为空")
    private Integer id;

    /** 商品分类ID */
    private Integer categoryId;

    /** 商品标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 商品价格 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 新旧程度 */
    private Integer condition;

    /** 交易方式 */
    private Integer tradeType;

    /** 库存数量 */
    private Integer stock;

    /** 商品图片列表（全量替换） */
    private List<ProductImageDTO> images;
}
