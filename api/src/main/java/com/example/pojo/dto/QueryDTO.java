package com.example.pojo.dto;

import lombok.Data;

import java.util.List;

/**
 * 查询条件基类
 **/
@Data
public class QueryDTO {
    private Integer id; // 主键ID
    private List<Integer> ids; // 主键ID列表
    private Integer current; // 当前页
    private Integer size; // 页面大小

    public static int pager(Integer current, Integer size) {
        if (current == null || size == null) {
            return 0;
        }
        return (current - 1) * size;
    }

}
