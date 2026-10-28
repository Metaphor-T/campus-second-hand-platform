package com.example.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单参数
 */
@Data
public class OrderCreateDTO {

    /** 商品ID */
    @NotNull(message = "商品ID不能为空")
    private Integer productId;

    /** 收货地址ID（交易方式为邮寄时必填） */
    private Integer addressId;

    /** 购买数量 */
    @Min(value = 1, message = "购买数量不能少于1")
    private Integer quantity;

    /** 买家备注 */
    private String buyerRemark;
}
