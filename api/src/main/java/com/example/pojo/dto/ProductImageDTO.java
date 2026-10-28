package com.example.pojo.dto;

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
    private String imageUrl;

    /** 是否为封面图：1-是，0-否 */
    private Integer isCover;

    /** 排序权重 */
    private Integer sort;
}
