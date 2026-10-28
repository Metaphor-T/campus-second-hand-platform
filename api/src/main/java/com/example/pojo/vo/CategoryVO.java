package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商品分类视图对象 - 返回给前端的数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryVO {

    /** 分类主键ID */
    private Integer id;

    /** 父分类ID，0表示顶级分类 */
    private Integer parentId;

    /** 分类名称 */
    private String name;

    /** 排序权重 */
    private Integer sort;

    /** 状态：1-启用，0-禁用 */
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
