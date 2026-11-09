package com.example.pojo.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品图片参数（用于新增/修改）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDTO {

    /** 图片URL地址 */
    @NotBlank(message = "图片地址不能为空")
    @Size(max = 255, message = "图片地址过长")
    private String imageUrl;

    /** 是否为封面图：1-是，0-否 */
    @Min(0)
    @Max(1)
    private Integer isCover;

    /** 排序权重 */
    @PositiveOrZero(message = "图片排序无效")
    private Integer sort;
}
