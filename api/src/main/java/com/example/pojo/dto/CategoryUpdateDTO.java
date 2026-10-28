package com.example.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改商品分类参数
 * <p>所有字段均可选（仅传需要修改的字段），后端只更新非 null 字段</p>
 */
@Data
public class CategoryUpdateDTO {

    /** 分类主键ID */
    @NotNull(message = "ID不能为空")
    private Integer id;

    /** 父分类ID */
    private Integer parentId;

    /** 分类名称 */
    private String name;

    /** 排序权重 */
    private Integer sort;

    /** 状态：1-启用，0-禁用 */
    private Integer status;
}
