package com.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 统计分析持久化接口
 */
@Mapper
public interface StatisticsMapper {

    /**
     * 获取所有用户总数
     */
    Integer selectTotalUsers();

    /**
     * 获取今天新增用户数
     */
    Integer selectTodayNewUsers();

    /**
     * 查询近N天每日新增用户数
     */
    List<Map<String, Object>> selectDailyNewUsers(@Param("days") Integer days);

    /**
     * 获取商品总数
     */
    Integer selectTotalProducts();

    /**
     * 获取待审核商品数
     */
    Integer selectPendingReviewProducts();

    /**
     * 获取订单总数
     */
    Integer selectTotalOrders();

    /**
     * 获取今日新增订单数
     */
    Integer selectTodayNewOrders();

    /**
     * 获取已完成订单总成交额
     */
    BigDecimal selectTotalRevenue();

    /**
     * 获取待处理退款订单数
     */
    Integer selectPendingRefundOrders();

    /**
     * 查询近N天每日新增订单数
     */
    List<Map<String, Object>> selectDailyNewOrders(@Param("days") Integer days);

    /**
     * 查询各分类销量排行
     */
    List<Map<String, Object>> selectCategorySales();

    /**
     * 查询各订单状态数量
     */
    List<Map<String, Object>> selectOrderStatusCount();

    /**
     * 获取所有商品平均评分
     */
    BigDecimal selectAverageRating();

    /**
     * 获取有交易的用户数（买或卖）
     */
    Integer selectActiveUserCount();

    /**
     * 获取所有用户中发布过商品的用户数
     */
    Integer selectProductPublisherCount();

    /**
     * 获取平均评分大于等于4的商品数量
     */
    Integer selectHighRatedProductCount();

    /**
     * 获取已完成订单中有评价的订单数
     */
    Integer selectReviewedOrderCount();
}
