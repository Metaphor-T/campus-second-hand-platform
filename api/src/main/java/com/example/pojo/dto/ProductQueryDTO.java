package com.example.pojo.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import lombok.EqualsAndHashCode;

/**
 * 商品分页查询参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductQueryDTO extends QueryDTO {

    public ProductQueryDTO() { setCurrent(1); setSize(12); }

    @Override
    @Min(value = 1, message = "页码至少为1")
    @Max(value = 100000, message = "页码过大，请缩小筛选范围")
    public Integer getCurrent() { return super.getCurrent(); }

    @Override
    @Min(value = 1, message = "每页至少1条")
    @Max(value = 50, message = "每页最多50条")
    public Integer getSize() { return super.getSize(); }

    /** 排序仅接受固定选项，SQL不拼接用户输入 */
    @Pattern(regexp = "latest|priceAsc|priceDesc|hot", message = "排序方式无效")
    private String sortBy = "latest";

    @AssertTrue(message = "最低价格不能高于最高价格")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }

    /** 用户ID（查询某用户发布的商品） */
    @Positive(message = "查询编号无效")
    private Integer userId;

    /** 商品分类ID */
    @Positive(message = "查询编号无效")
    private Integer categoryId;

    /** 商品标题（模糊搜索） */
    @Size(max = 200, message = "关键词最多200字")
    private String title;

    /** 审核状态筛选：0-待审核，1-审核通过，2-审核拒绝，3-已下架 */
    @Min(0)
    @Max(3)
    private Integer status;

    /** 新旧程度筛选 */
    @Min(1)
    @Max(4)
    private Integer condition;

    /** 交易方式筛选 */
    @Min(1)
    @Max(3)
    private Integer tradeType;

    /** 最低价格 */
    @DecimalMin(value = "0", message = "筛选价格不能为负数")
    @Digits(integer = 8, fraction = 2, message = "筛选价格最多8位整数、2位小数")
    private java.math.BigDecimal minPrice;

    /** 最高价格 */
    @DecimalMin(value = "0", message = "筛选价格不能为负数")
    @Digits(integer = 8, fraction = 2, message = "筛选价格最多8位整数、2位小数")
    private java.math.BigDecimal maxPrice;
}
