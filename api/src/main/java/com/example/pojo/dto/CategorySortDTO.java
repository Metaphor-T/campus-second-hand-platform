package com.example.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品分类排序参数 - 用于拖拽排序后批量更新
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySortDTO {

    /** 分类主键ID */
    private Integer id;

    /** 父分类ID */
    private Integer parentId;

    /** 排序权重 */
    private Integer sort;

    /** 层级深度 */
    private Integer level;
}
