package com.example.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增商品分类参数
 */
@Data
public class CategoryAddDTO {

    /** 父分类ID，不传或为0表示顶级分类 */
    private Integer parentId;

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /** 排序权重，数值越大越靠前 */
    private Integer sort;

    /** 状态：1-启用，0-禁用 */
    private Integer status;
}
