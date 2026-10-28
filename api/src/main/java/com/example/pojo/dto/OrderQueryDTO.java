package com.example.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单分页查询参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OrderQueryDTO extends QueryDTO {

    /** 订单状态筛选（可选） */
    private Integer orderStatus;

    /** 搜索关键词（订单编号模糊搜索） */
    private String keyword;
}
