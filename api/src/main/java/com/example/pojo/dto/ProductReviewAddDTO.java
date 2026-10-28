package com.example.pojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 新增商品评价参数
 */
@Data
public class ProductReviewAddDTO {

    /** 订单ID */
    @NotNull(message = "订单ID不能为空")
    private Integer orderId;

    /** 评分：1-5星 */
    @NotNull(message = "请给出评分")
    @Min(value = 1, message = "评分最低为1星")
    @Max(value = 5, message = "评分最高为5星")
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 评价图片URL列表 */
    private List<String> images;
}
