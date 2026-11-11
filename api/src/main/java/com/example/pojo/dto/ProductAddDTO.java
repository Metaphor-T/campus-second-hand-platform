package com.example.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
    @Positive(message = "商品分类无效")
    private Integer categoryId;

    /** 商品标题 */
    @NotBlank(message = "商品标题不能为空")
    @Size(max = 200, message = "商品标题最多200字")
    private String title;

    /** 商品描述 */
    @Size(max = 2000, message = "商品描述最多2000字")
    private String description;

    /** 商品价格 */
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "售价至少为0.01元")
    @Digits(integer = 8, fraction = 2, message = "售价最多8位整数、2位小数")
    private BigDecimal price;

    /** 原价（选填） */
    @DecimalMin(value = "0", message = "原价不能为负数")
    @Digits(integer = 8, fraction = 2, message = "原价最多8位整数、2位小数")
    private BigDecimal originalPrice;

    /** 新旧程度：1-全新，2-几乎全新，3-有使用痕迹，4-老旧 */
    @NotNull(message = "请选择新旧程度")
    @Min(value = 1, message = "新旧程度无效")
    @Max(value = 4, message = "新旧程度无效")
    private Integer condition;

    /** 交易方式：1-自提，2-邮寄，3-两者皆可 */
    @NotNull(message = "请选择交易方式")
    @Min(value = 1, message = "交易方式无效")
    @Max(value = 3, message = "交易方式无效")
    private Integer tradeType;

    /** 库存模式：0有限库存，1不限库存 */
    @NotNull(message = "请选择库存模式")
    @Min(value = 0, message = "库存模式无效")
    @Max(value = 1, message = "库存模式无效")
    private Integer stockMode;

    /** 库存数量；不限库存时为0 */
    @NotNull(message = "请填写库存数量")
    @Min(value = 0, message = "库存不能为负数")
    @Max(value = 999999, message = "库存最多999999件")
    private Integer stock;

    /** 商品图片列表（图册） */
    @NotNull(message = "请上传商品图片")
    @Valid
    @Size(min = 1, max = 9, message = "请保留1至9张商品图片")
    private List<ProductImageDTO> images;
}
