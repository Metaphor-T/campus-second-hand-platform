package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.exception.BusinessException;
import com.example.mapper.OrderMapper;
import com.example.pojo.dto.OrderCreateDTO;
import com.example.pojo.dto.OrderQueryDTO;
import com.example.pojo.entity.Order;
import com.example.pojo.entity.Product;
import com.example.pojo.vo.OrderVO;
import com.example.service.OrderService;
import com.example.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 订单业务逻辑接口实现类
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderMapper orderMapper;
    private final ProductService productService;

    /**
     * 生成唯一订单编号（含时间戳和随机串）
     */
    private String generateOrderNo() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD" + timestamp + uuid;
    }

    /**
     * 创建订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Integer buyerId, OrderCreateDTO createDTO) {
        // 1. 校验商品是否存在且已审核通过
        Product product = productService.getById(createDTO.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException("商品暂未上架或审核未通过");
        }

        // 2. 不能购买自己的商品
        if (Objects.equals(product.getUserId(), buyerId)) {
            throw new BusinessException("不能购买自己发布的商品");
        }

        // 3. 校验交易方式下地址必填
        Integer tradeType = product.getTradeType();
        Integer quantity = createDTO.getQuantity() != null && createDTO.getQuantity() >= 1 ? createDTO.getQuantity() : 1;
        BigDecimal unitPrice = product.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal shippingFee = BigDecimal.ZERO;

        // 交易方式：2-邮寄时需要地址和运费；1-自提、3-两者皆可不需要地址
        if (tradeType == 2) {
            if (createDTO.getAddressId() == null) {
                throw new BusinessException("该商品需要邮寄，请选择收货地址");
            }
            // 邮寄场景可设置少量运费（示例固定6元）
            shippingFee = new BigDecimal("6.00");
        }

        // 3.5 库存校验与扣减（乐观锁防超卖）
        if (product.getStock() != null && product.getStock() > 0) {
            // 限库存商品：校验库存充足
            if (product.getStock() < quantity) {
                throw new BusinessException("商品库存不足，当前库存：" + product.getStock());
            }
            // 乐观锁扣减库存（传入当前版本号）
            boolean deducted = productService.deductStock(product.getId(), quantity, product.getVersion());
            if (!deducted) {
                throw new BusinessException("库存扣减失败，请稍后重试");
            }
        }
        // stock == 0 表示不限库存，无需扣减

        BigDecimal payAmount = totalAmount.add(shippingFee);

        // 4. 创建订单
        Order order = Order.builder()
                .orderNo(generateOrderNo())
                .productId(createDTO.getProductId())
                .buyerId(buyerId)
                .sellerId(product.getUserId())
                .addressId(createDTO.getAddressId())
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .shippingFee(shippingFee)
                .payAmount(payAmount)
                .orderStatus(0) // 待付款
                .buyerRemark(createDTO.getBuyerRemark())
                .createTime(LocalDateTime.now())
                .build();
        this.save(order);

        return orderMapper.getDetail(order.getId());
    }

    /**
     * 买家订单列表
     */
    @Override
    public List<OrderVO> listBuyerOrders(Integer buyerId, OrderQueryDTO queryDTO) {
        return orderMapper.listBuyerOrders(buyerId, queryDTO);
    }

    /**
     * 买家订单总数
     */
    @Override
    public Integer listBuyerOrdersCount(Integer buyerId, OrderQueryDTO queryDTO) {
        return orderMapper.listBuyerOrdersCount(buyerId, queryDTO);
    }

    /**
     * 卖家订单列表
     */
    @Override
    public List<OrderVO> listSellerOrders(Integer sellerId, OrderQueryDTO queryDTO) {
        return orderMapper.listSellerOrders(sellerId, queryDTO);
    }

    /**
     * 卖家订单总数
     */
    @Override
    public Integer listSellerOrdersCount(Integer sellerId, OrderQueryDTO queryDTO) {
        return orderMapper.listSellerOrdersCount(sellerId, queryDTO);
    }

    /**
     * 查询订单详情
     */
    @Override
    public OrderVO getDetail(Integer orderId) {
        OrderVO vo = orderMapper.getDetail(orderId);
        if (vo == null) {
            throw new BusinessException("订单不存在");
        }
        return vo;
    }

    /**
     * 买家取消订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Integer buyerId, Integer orderId, String reason) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!Objects.equals(order.getBuyerId(), buyerId)) {
            throw new BusinessException("只能取消自己的订单");
        }
        if (order.getOrderStatus() != 0) {
            throw new BusinessException("当前订单状态不允许取消");
        }
        order.setOrderStatus(4); // 已取消
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        this.updateById(order);

        // 归还库存（订单创建时若已扣减，则回滚）
        productService.restoreStock(order.getProductId(), order.getQuantity());
    }

    /**
     * 买家模拟支付
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Integer buyerId, Integer orderId, Integer payMethod) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!Objects.equals(order.getBuyerId(), buyerId)) {
            throw new BusinessException("只能支付自己的订单");
        }
        if (order.getOrderStatus() != 0) {
            throw new BusinessException("当前订单状态不允许支付");
        }
        // 校验支付方式
        if (payMethod == null || payMethod < 1 || payMethod > 3) {
            throw new BusinessException("无效的支付方式");
        }
        order.setOrderStatus(1); // 待发货
        order.setPayMethod(payMethod);
        order.setPayTime(LocalDateTime.now());
        this.updateById(order);
    }

    /**
     * 卖家发货
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Integer sellerId, Integer orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!Objects.equals(order.getSellerId(), sellerId)) {
            throw new BusinessException("只能操作自己的订单");
        }
        if (order.getOrderStatus() != 1) {
            throw new BusinessException("当前订单状态不允许发货");
        }
        order.setOrderStatus(2); // 待收货
        order.setShipTime(LocalDateTime.now());
        this.updateById(order);
    }

    /**
     * 买家确认收货
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveOrder(Integer buyerId, Integer orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!Objects.equals(order.getBuyerId(), buyerId)) {
            throw new BusinessException("只能确认自己的收货");
        }
        if (order.getOrderStatus() != 2) {
            throw new BusinessException("当前订单状态不允许确认收货");
        }
        order.setOrderStatus(3); // 已完成
        order.setReceiveTime(LocalDateTime.now());
        this.updateById(order);
    }

    /**
     * 卖家同意退款（将退款/售后订单改为已取消）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void agreeRefundOrder(Integer sellerId, Integer orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!Objects.equals(order.getSellerId(), sellerId)) {
            throw new BusinessException("只能操作自己的订单");
        }
        if (order.getOrderStatus() != 5) {
            throw new BusinessException("当前订单状态不允许同意退款");
        }
        order.setOrderStatus(4); // 已取消
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("卖家同意退款");
        this.updateById(order);

        // 归还库存（退款成功，库存回滚）
        productService.restoreStock(order.getProductId(), order.getQuantity());
    }

    /**
     * 买家退款/售后
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Integer buyerId, Integer orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!Objects.equals(order.getBuyerId(), buyerId)) {
            throw new BusinessException("只能申请退款自己的订单");
        }
        // 已发货、已收货状态可申请退款
        if (order.getOrderStatus() != 2 && order.getOrderStatus() != 3) {
            throw new BusinessException("当前订单状态不允许申请退款");
        }
        order.setOrderStatus(5); // 退款/售后
        this.updateById(order);
    }

    // ==================== 按状态统计数量 ====================

    /**
     * 统计买家各状态订单数量
     */
    @Override
    public Map<Integer, Integer> countBuyerOrdersByStatus(Integer buyerId) {
        List<Map<String, Object>> list = orderMapper.countBuyerOrdersByStatus(buyerId);
        Map<Integer, Integer> result = new HashMap<>();
        for (Map<String, Object> item : list) {
            Integer status = ((Number) item.get("status")).intValue();
            Integer count = ((Number) item.get("count")).intValue();
            result.put(status, count);
        }
        return result;
    }

    /**
     * 统计卖家各状态订单数量
     */
    @Override
    public Map<Integer, Integer> countSellerOrdersByStatus(Integer sellerId) {
        List<Map<String, Object>> list = orderMapper.countSellerOrdersByStatus(sellerId);
        Map<Integer, Integer> result = new HashMap<>();
        for (Map<String, Object> item : list) {
            Integer status = ((Number) item.get("status")).intValue();
            Integer count = ((Number) item.get("count")).intValue();
            result.put(status, count);
        }
        return result;
    }

    // ==================== 管理员端 ====================

    /**
     * 管理员分页查询全部订单
     */
    @Override
    public List<OrderVO> listAllOrders(OrderQueryDTO queryDTO) {
        return orderMapper.listAllOrders(queryDTO);
    }

    /**
     * 管理员查询全部订单总数
     */
    @Override
    public Integer listAllOrdersCount(OrderQueryDTO queryDTO) {
        return orderMapper.listAllOrdersCount(queryDTO);
    }

    /**
     * 管理员同意退款（将退款/售后订单改为已取消）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminAgreeRefundOrder(Integer adminId, Integer orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getOrderStatus() != 5) {
            throw new BusinessException("当前订单状态不允许同意退款");
        }
        order.setOrderStatus(4); // 已取消
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("管理员同意退款");
        this.updateById(order);

        // 归还库存（退款成功，库存回滚）
        productService.restoreStock(order.getProductId(), order.getQuantity());
    }
}
