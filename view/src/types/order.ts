// src/types/order.ts 订单类型文件

/**
 * 订单视图对象
 */
export interface OrderVO {
    id?: number                // 订单主键ID
    orderNo?: string           // 订单编号
    productId?: number         // 商品ID
    productTitle?: string      // 商品标题
    productImage?: string      // 商品首图
    buyerId?: number           // 买家用户ID
    buyerName?: string         // 买家用户名
    sellerId?: number          // 卖家用户ID
    sellerName?: string        // 卖家用户名
    addressId?: number         // 收货地址ID
    receiverName?: string      // 收货人姓名
    receiverPhone?: string     // 收货人手机号
    fullAddress?: string       // 完整收货地址
    quantity?: number          // 购买数量
    unitPrice?: number         // 成交单价
    totalAmount?: number       // 订单总金额
    shippingFee?: number       // 运费
    payAmount?: number         // 实付金额
    payMethod?: number         // 支付方式：1微信，2支付宝，3线下交易
    payTime?: string           // 支付时间
    orderStatus?: number       // 订单状态：0待付款，1待发货，2待收货，3已完成，4已取消，5退款/售后
    shipTime?: string          // 发货时间
    receiveTime?: string       // 收货时间
    cancelTime?: string        // 取消时间
    cancelReason?: string      // 取消原因
    buyerRemark?: string       // 买家备注
    createTime?: string        // 下单时间
    updateTime?: string        // 更新时间
}

/**
 * 创建订单参数
 */
export interface OrderCreateDTO {
    productId?: number         // 商品ID
    addressId?: number         // 收货地址ID
    quantity?: number          // 购买数量
    buyerRemark?: string       // 买家备注
}

/**
 * 订单查询参数
 */
export interface OrderQueryDTO {
    current?: number           // 当前页
    size?: number              // 页面大小
    orderStatus?: number       // 订单状态筛选
    keyword?: string           // 搜索关键词
}
