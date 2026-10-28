package com.example.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 卖家回复评价参数
 */
@Data
public class ProductReviewReplyDTO {

    /** 评价ID */
    @NotNull(message = "评价ID不能为空")
    private Integer id;

    /** 回复内容 */
    @NotBlank(message = "回复内容不能为空")
    private String replyContent;
}
