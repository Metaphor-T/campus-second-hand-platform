package com.example.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
    @Positive(message = "商品ID无效")
    private Integer id;

    /** 编辑时读取的版本号；旧客户端可省略 */
    @PositiveOrZero(message = "版本号无效")
    private Integer version;

    /** 商品分类ID */
    @Positive(message = "商品分类无效")
    private Integer categoryId;

    /** 商品标题 */
    @Pattern(regexp = "(?s).*\\S.*", message = "商品标题不能全为空格")
    @Size(max = 200, message = "商品标题最多200字")
    private String title;

    /** 商品描述 */
    @Size(max = 2000, message = "商品描述最多2000字")
    private String description;

    /** 商品价格 */
    @DecimalMin(value = "0.01", message = "售价至少为0.01元")
    @Digits(integer = 8, fraction = 2, message = "售价最多8位整数、2位小数")
    private BigDecimal price;

    /** 原价 */
    @DecimalMin(value = "0", message = "原价不能为负数")
    @Digits(integer = 8, fraction = 2, message = "原价最多8位整数、2位小数")
    private BigDecimal originalPrice;

    /** true 表示清空原价，未提供时保持旧客户端的局部更新语义 */
    private Boolean clearOriginalPrice;

    /** 新旧程度 */
    @Min(value = 1, message = "新旧程度无效")
    @Max(value = 4, message = "新旧程度无效")
    private Integer condition;

    /** 交易方式 */
    @Min(value = 1, message = "交易方式无效")
    @Max(value = 3, message = "交易方式无效")
    private Integer tradeType;

    /** 库存模式：0有限库存，1不限库存 */
    @Min(value = 0, message = "库存模式无效")
    @Max(value = 1, message = "库存模式无效")
    private Integer stockMode;

    /** 库存数量 */
    @Min(value = 0, message = "库存不能为负数")
    @Max(value = 999999, message = "库存最多999999件")
    private Integer stock;

    /** 商品图片列表（全量替换） */
    @Valid
    @Size(min = 1, max = 9, message = "请保留1至9张商品图片")
    private List<ProductImageDTO> images;
}
