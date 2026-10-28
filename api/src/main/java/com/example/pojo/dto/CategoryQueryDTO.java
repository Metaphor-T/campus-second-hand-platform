package com.example.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分类分页查询参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CategoryQueryDTO extends QueryDTO {

    /** 分类名称（模糊搜索） */
    private String name;

    /** 父分类ID，查询某分类下的子分类 */
    private Integer parentId;

    /** 状态筛选：1-启用，0-禁用 */
    private Integer status;
}
