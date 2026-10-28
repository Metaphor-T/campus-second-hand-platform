package com.example.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增商品参数
 */
@Data
public class ProductAddDTO {

    /** 商品分类ID */
    @NotNull(message = "请选择商品分类")
    private Integer categoryId;

    /** 商品标题 */
    @NotBlank(message = "商品标题不能为空")
    private String title;

    /** 商品描述 */
    private String description;

    /** 商品价格 */
    @NotNull(message = "商品价格不能为空")
    private BigDecimal price;

    /** 原价（选填） */
    private BigDecimal originalPrice;

    /** 新旧程度：1-全新，2-几乎全新，3-有使用痕迹，4-老旧 */
    @NotNull(message = "请选择新旧程度")
    private Integer condition;

    /** 交易方式：1-自提，2-邮寄，3-两者皆可 */
    @NotNull(message = "请选择交易方式")
    private Integer tradeType;

    /** 库存数量（0表示不限库存，默认0） */
    private Integer stock;

    /** 商品图片列表（图册） */
    private List<ProductImageDTO> images;
}
