package com.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体 - 对应数据库表 `order`
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "`order`")
public class Order {

    /** 订单主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 订单编号（唯一） */
    private String orderNo;

    /** 商品ID（关联 product.id） */
    private Integer productId;

    /** 买家用户ID（关联 base_user.id） */
    private Integer buyerId;

    /** 卖家用户ID（关联 base_user.id） */
    private Integer sellerId;

    /** 收货地址ID（关联 address.id，邮寄时必填） */
    private Integer addressId;

    /** 购买数量 */
    private Integer quantity;

    /** 成交单价（下单时价格快照） */
    private BigDecimal unitPrice;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 运费 */
    private BigDecimal shippingFee;

    /** 实付金额（总金额+运费） */
    private BigDecimal payAmount;

    /** 支付方式：1-微信，2-支付宝，3-线下交易 */
    private Integer payMethod;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime payTime;

    /** 订单状态：0-待付款，1-待发货，2-待收货，3-已完成，4-已取消，5-退款/售后 */
    private Integer orderStatus;

    /** 发货时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime shipTime;

    /** 收货时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime receiveTime;

    /** 取消时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime cancelTime;

    /** 取消原因 */
    private String cancelReason;

    /** 买家备注 */
    private String buyerRemark;

    /** 下单时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
