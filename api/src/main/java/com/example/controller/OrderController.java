package com.example.controller;

import com.example.annotation.Auth;
import com.example.annotation.Pager;
import com.example.pojo.api.Result;
import com.example.pojo.dto.OrderCreateDTO;
import com.example.pojo.dto.OrderQueryDTO;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.OrderVO;
import com.example.service.OrderService;
import com.example.utils.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 * <p>用户端：创建订单、支付、取消、确认收货、退款</p>
 * <p>卖家端：查看订单、发货</p>
 * <p>权限隔离：买家只能操作自己的订单，卖家只能操作自己的订单</p>
 */
@RestController
@RequestMapping(value = "/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ==================== 创建订单 ====================

    /**
     * 创建订单（买家下单）
     *
     * @param createDTO 下单参数
     * @return 订单VO
     */
    @Auth({RoleEnum.USER})
    @PostMapping(value = "/create")
    public Result<OrderVO> createOrder(@Valid @RequestBody OrderCreateDTO createDTO) {
        Integer buyerId = UserContext.getUserId();
        OrderVO vo = orderService.createOrder(buyerId, createDTO);
        return Result.success(vo);
    }

    // ==================== 买家端接口 ====================

    /**
     * 买家分页查询自己的订单
     *
     * @param queryDTO 查询参数（支持按状态筛选、订单号模糊搜索）
     * @return 分页订单列表
     */
    @Auth({RoleEnum.USER})
    @Pager
    @PostMapping(value = "/buyer/list")
    public Result<List<OrderVO>> listBuyerOrders(@RequestBody OrderQueryDTO queryDTO) {
        Integer buyerId = UserContext.getUserId();
        List<OrderVO> list = orderService.listBuyerOrders(buyerId, queryDTO);
        Integer count = orderService.listBuyerOrdersCount(buyerId, queryDTO);
        return Result.success(list, count);
    }

    /**
     * 买家取消订单（仅待付款状态可取消）
     *
     * @param orderId 订单ID
     * @param reason  取消原因（可选）
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PutMapping(value = "/cancel/{orderId}")
    public Result<Void> cancelOrder(@PathVariable Integer orderId, @RequestParam(required = false) String reason) {
        Integer buyerId = UserContext.getUserId();
        orderService.cancelOrder(buyerId, orderId, reason);
        return Result.success();
    }

    /**
     * 买家模拟支付
     *
     * @param orderId   订单ID
     * @param payMethod 支付方式：1-微信，2-支付宝，3-线下交易
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PutMapping(value = "/pay/{orderId}")
    public Result<Void> payOrder(@PathVariable Integer orderId, @RequestParam Integer payMethod) {
        Integer buyerId = UserContext.getUserId();
        orderService.payOrder(buyerId, orderId, payMethod);
        return Result.success();
    }

    /**
     * 买家确认收货
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PutMapping(value = "/receive/{orderId}")
    public Result<Void> receiveOrder(@PathVariable Integer orderId) {
        Integer buyerId = UserContext.getUserId();
        orderService.receiveOrder(buyerId, orderId);
        return Result.success();
    }

    /**
     * 买家申请退款/售后
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PutMapping(value = "/refund/{orderId}")
    public Result<Void> refundOrder(@PathVariable Integer orderId) {
        Integer buyerId = UserContext.getUserId();
        orderService.refundOrder(buyerId, orderId);
        return Result.success();
    }

    // ==================== 卖家端接口 ====================

    /**
     * 卖家分页查询自己的订单
     *
     * @param queryDTO 查询参数
     * @return 分页订单列表
     */
    @Auth({RoleEnum.USER})
    @Pager
    @PostMapping(value = "/seller/list")
    public Result<List<OrderVO>> listSellerOrders(@RequestBody OrderQueryDTO queryDTO) {
        Integer sellerId = UserContext.getUserId();
        List<OrderVO> list = orderService.listSellerOrders(sellerId, queryDTO);
        Integer count = orderService.listSellerOrdersCount(sellerId, queryDTO);
        return Result.success(list, count);
    }

    /**
     * 卖家同意退款
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PutMapping(value = "/agree-refund/{orderId}")
    public Result<Void> agreeRefundOrder(@PathVariable Integer orderId) {
        Integer sellerId = UserContext.getUserId();
        orderService.agreeRefundOrder(sellerId, orderId);
        return Result.success();
    }

    /**
     * 卖家发货
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PutMapping(value = "/ship/{orderId}")
    public Result<Void> shipOrder(@PathVariable Integer orderId) {
        Integer sellerId = UserContext.getUserId();
        orderService.shipOrder(sellerId, orderId);
        return Result.success();
    }

    // ==================== 公共接口 ====================

    /**
     * 查询订单详情（买家或卖家均可查看自己的订单）
     *
     * @param orderId 订单ID
     * @return 订单VO
     */
    @Auth({RoleEnum.USER})
    @GetMapping(value = "/detail/{orderId}")
    public Result<OrderVO> detail(@PathVariable Integer orderId) {
        return Result.success(orderService.getDetail(orderId));
    }

    // ==================== 各状态订单数量统计 ====================

    /**
     * 查询买家各状态订单数量
     *
     * @return Map，key=状态值，value=数量（如 {0:3, 1:1, 3:5}）
     */
    @Auth({RoleEnum.USER})
    @GetMapping(value = "/buyer/count-by-status")
    public Result<Map<Integer, Integer>> countBuyerOrdersByStatus() {
        Integer buyerId = UserContext.getUserId();
        Map<Integer, Integer> map = orderService.countBuyerOrdersByStatus(buyerId);
        return Result.success(map);
    }

    /**
     * 查询卖家各状态订单数量
     *
     * @return Map，key=状态值，value=数量
     */
    @Auth({RoleEnum.USER})
    @GetMapping(value = "/seller/count-by-status")
    public Result<Map<Integer, Integer>> countSellerOrdersByStatus() {
        Integer sellerId = UserContext.getUserId();
        Map<Integer, Integer> map = orderService.countSellerOrdersByStatus(sellerId);
        return Result.success(map);
    }

    // ==================== 管理员端接口 ====================

    /**
     * 管理员分页查询全部订单
     *
     * @param queryDTO 查询参数（支持按状态筛选、关键词搜索订单号/商品名/用户名）
     * @return 分页订单列表
     */
    @Auth({RoleEnum.ADMIN})
    @Pager
    @PostMapping(value = "/admin/list")
    public Result<List<OrderVO>> listAllOrders(@RequestBody OrderQueryDTO queryDTO) {
        List<OrderVO> list = orderService.listAllOrders(queryDTO);
        Integer count = orderService.listAllOrdersCount(queryDTO);
        return Result.success(list, count);
    }

    /**
     * 管理员同意退款（退款审核通过）
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @Auth({RoleEnum.ADMIN})
    @PutMapping(value = "/admin/agree-refund/{orderId}")
    public Result<Void> adminAgreeRefundOrder(@PathVariable Integer orderId) {
        Integer adminId = UserContext.getUserId();
        orderService.adminAgreeRefundOrder(adminId, orderId);
        return Result.success();
    }
}
