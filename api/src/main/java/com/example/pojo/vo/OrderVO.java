package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图对象 - 返回给前端的数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderVO {

    /** 订单主键ID */
    private Integer id;

    /** 订单编号 */
    private String orderNo;

    /** 商品ID */
    private Integer productId;

    /** 商品标题 */
    private String productTitle;

    /** 商品首图 */
    private String productImage;

    /** 买家用户ID */
    private Integer buyerId;

    /** 买家用户名 */
    private String buyerName;

    /** 卖家用户ID */
    private Integer sellerId;

    /** 卖家用户名 */
    private String sellerName;

    /** 收货地址ID */
    private Integer addressId;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人手机号 */
    private String receiverPhone;

    /** 收货地址（完整拼接） */
    private String fullAddress;

    /** 购买数量 */
    private Integer quantity;

    /** 成交单价 */
    private BigDecimal unitPrice;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 运费 */
    private BigDecimal shippingFee;

    /** 实付金额 */
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
