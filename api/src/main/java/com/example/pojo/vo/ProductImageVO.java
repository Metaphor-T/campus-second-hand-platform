package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品图片视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductImageVO {

    /**
     * 图片主键ID
     */
    private Integer id;

    /**
     * 商品ID
     */
    private Integer productId;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 是否为封面：1-是，0-否
     */
    private Integer isCover;

    /**
     * 排序
     */
    private Integer sort;
}
