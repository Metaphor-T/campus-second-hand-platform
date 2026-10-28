package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.pojo.dto.OrderCreateDTO;
import com.example.pojo.dto.OrderQueryDTO;
import com.example.pojo.entity.Order;
import com.example.pojo.vo.OrderVO;

import java.util.List;
import java.util.Map;

/**
 * 订单业务逻辑接口
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建订单
     *
     * @param buyerId 买家ID
     * @param createDTO 下单参数
     * @return 订单VO
     */
    OrderVO createOrder(Integer buyerId, OrderCreateDTO createDTO);

    /**
     * 买家分页查询自己的订单
     *
     * @param buyerId  买家ID
     * @param queryDTO 查询参数
     * @return 订单VO列表
     */
    List<OrderVO> listBuyerOrders(Integer buyerId, OrderQueryDTO queryDTO);

    /**
     * 买家订单总数
     *
     * @param buyerId  买家ID
     * @param queryDTO 查询参数
     * @return 总数
     */
    Integer listBuyerOrdersCount(Integer buyerId, OrderQueryDTO queryDTO);

    /**
     * 卖家分页查询自己的订单
     *
     * @param sellerId 卖家ID
     * @param queryDTO 查询参数
     * @return 订单VO列表
     */
    List<OrderVO> listSellerOrders(Integer sellerId, OrderQueryDTO queryDTO);

    /**
     * 卖家订单总数
     *
     * @param sellerId 卖家ID
     * @param queryDTO 查询参数
     * @return 总数
     */
    Integer listSellerOrdersCount(Integer sellerId, OrderQueryDTO queryDTO);

    /**
     * 查询订单详情
     *
     * @param orderId 订单ID
     * @return 订单VO
     */
    OrderVO getDetail(Integer orderId);

    /**
     * 买家取消订单（仅待付款状态可取消）
     *
     * @param buyerId  买家ID
     * @param orderId  订单ID
     * @param reason   取消原因
     */
    void cancelOrder(Integer buyerId, Integer orderId, String reason);

    /**
     * 买家模拟支付订单
     *
     * @param buyerId   买家ID
     * @param orderId   订单ID
     * @param payMethod 支付方式
     */
    void payOrder(Integer buyerId, Integer orderId, Integer payMethod);

    /**
     * 卖家发货
     *
     * @param sellerId 卖家ID
     * @param orderId  订单ID
     */
    void shipOrder(Integer sellerId, Integer orderId);

    /**
     * 买家确认收货
     *
     * @param buyerId 买家ID
     * @param orderId 订单ID
     */
    void receiveOrder(Integer buyerId, Integer orderId);

    /**
     * 买家申请退款/售后
     *
     * @param buyerId 买家ID
     * @param orderId 订单ID
     */
    void refundOrder(Integer buyerId, Integer orderId);

    /**
     * 卖家同意退款
     *
     * @param sellerId 卖家ID
     * @param orderId  订单ID
     */
    void agreeRefundOrder(Integer sellerId, Integer orderId);

    /**
     * 统计买家各状态订单数量
     *
     * @param buyerId 买家ID
     * @return Map，key 为状态值（Integer），value 为数量
     */
    Map<Integer, Integer> countBuyerOrdersByStatus(Integer buyerId);

    /**
     * 统计卖家各状态订单数量
     *
     * @param sellerId 卖家ID
     * @return Map，key 为状态值（Integer），value 为数量
     */
    Map<Integer, Integer> countSellerOrdersByStatus(Integer sellerId);

    // ==================== 管理员端 ====================

    /**
     * 管理员分页查询全部订单
     *
     * @param queryDTO 查询参数
     * @return 订单VO列表
     */
    List<OrderVO> listAllOrders(OrderQueryDTO queryDTO);

    /**
     * 管理员查询全部订单总数
     *
     * @param queryDTO 查询参数
     * @return 总数
     */
    Integer listAllOrdersCount(OrderQueryDTO queryDTO);

    /**
     * 管理员同意退款（退款审核通过，将退款/售后订单改为已取消）
     *
     * @param adminId 管理员ID
     * @param orderId 订单ID
     */
    void adminAgreeRefundOrder(Integer adminId, Integer orderId);
}
