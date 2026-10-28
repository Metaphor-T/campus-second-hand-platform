package com.example.service;

import com.example.mapper.StatisticsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 统计分析业务逻辑
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsMapper statisticsMapper;

    /**
     * 概览统计
     */
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalUsers", statisticsMapper.selectTotalUsers());
        overview.put("totalProducts", statisticsMapper.selectTotalProducts());
        overview.put("totalOrders", statisticsMapper.selectTotalOrders());
        overview.put("totalRevenue", statisticsMapper.selectTotalRevenue());
        overview.put("pendingReviewProducts", statisticsMapper.selectPendingReviewProducts());
        overview.put("pendingRefundOrders", statisticsMapper.selectPendingRefundOrders());
        overview.put("todayNewUsers", statisticsMapper.selectTodayNewUsers());
        overview.put("todayNewOrders", statisticsMapper.selectTodayNewOrders());
        return overview;
    }

    /**
     * 近N天每日新增用户和订单趋势
     */
    public Map<String, Object> getTrends(Integer days) {
        if (days == null || days <= 0) {
            days = 7;
        }

        // 查询原始数据
        List<Map<String, Object>> userRaw = statisticsMapper.selectDailyNewUsers(days);
        List<Map<String, Object>> orderRaw = statisticsMapper.selectDailyNewOrders(days);

        // 转换为日期->数量Map
        Map<String, Integer> userMap = new LinkedHashMap<>();
        for (Map<String, Object> row : userRaw) {
            userMap.put(row.get("date").toString(), ((Number) row.get("count")).intValue());
        }
        Map<String, Integer> orderMap = new LinkedHashMap<>();
        for (Map<String, Object> row : orderRaw) {
            orderMap.put(row.get("date").toString(), ((Number) row.get("count")).intValue());
        }

        // 补全缺失日期（增序：从最远到最近）
        List<Map<String, Object>> userTrends = new ArrayList<>();
        List<Map<String, Object>> orderTrends = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            // 每次迭代创建新实例，避免 add() 累积导致日期偏移 bug
            Calendar day = Calendar.getInstance();
            day.set(Calendar.HOUR_OF_DAY, 0);
            day.set(Calendar.MINUTE, 0);
            day.set(Calendar.SECOND, 0);
            day.set(Calendar.MILLISECOND, 0);
            day.add(Calendar.DAY_OF_YEAR, -i);

            String dateStr = String.format("%tF", day);
            int userCount = userMap.getOrDefault(dateStr, 0);
            int orderCount = orderMap.getOrDefault(dateStr, 0);

            Map<String, Object> uItem = new LinkedHashMap<>();
            uItem.put("date", dateStr);
            uItem.put("count", userCount);
            userTrends.add(uItem);

            Map<String, Object> oItem = new LinkedHashMap<>();
            oItem.put("date", dateStr);
            oItem.put("count", orderCount);
            orderTrends.add(oItem);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userTrends", userTrends);
        result.put("orderTrends", orderTrends);
        return result;
    }

    /**
     * 各分类销量排行
     */
    public List<Map<String, Object>> getCategorySales() {
        return statisticsMapper.selectCategorySales();
    }

    /**
     * 订单状态分布
     */
    public List<Map<String, Object>> getOrderStatusDistribution() {
        List<Map<String, Object>> raw = statisticsMapper.selectOrderStatusCount();
        // 状态映射
        Map<Integer, String> statusMap = new HashMap<>();
        statusMap.put(0, "待付款");
        statusMap.put(1, "待发货");
        statusMap.put(2, "待收货");
        statusMap.put(3, "已完成");
        statusMap.put(4, "已取消");
        statusMap.put(5, "退款/售后");

        for (Map<String, Object> item : raw) {
            Integer status = toInt(item.get("status"));
            item.put("statusName", statusMap.getOrDefault(status, "未知"));
        }
        return raw;
    }

    /**
     * 雷达图 — 平台健康综合指标（满分100）
     */
    public Map<String, Object> getRadarData() {
        Integer totalUsers = statisticsMapper.selectTotalUsers();
        Integer totalProducts = statisticsMapper.selectTotalProducts();
        Integer totalOrders = statisticsMapper.selectTotalOrders();
        Integer activeUsers = statisticsMapper.selectActiveUserCount();
        Integer publishers = statisticsMapper.selectProductPublisherCount();

        BigDecimal avgRating = statisticsMapper.selectAverageRating();
        Integer highRatedCount = statisticsMapper.selectHighRatedProductCount();
        Integer totalApproved = statisticsMapper.selectTotalProducts() != null
                ? statisticsMapper.selectTotalProducts() : 0;

        // 用户活跃度: 有交易行为的用户占比
        double userActivity = totalUsers > 0
                ? round((double) activeUsers / totalUsers * 100, 1)
                : 0;

        // 商品丰富度: 发布商品用户占比
        double productRichness = totalUsers > 0
                ? round((double) publishers / totalUsers * 100, 1)
                : 0;

        // 交易完成率: 已完成订单占比
        Integer completedOrders = 0;
        for (Map<String, Object> item : statisticsMapper.selectOrderStatusCount()) {
            if (toInt(item.get("status")) == 3) {
                completedOrders = toInt(item.get("count"));
                break;
            }
        }
        double orderCompletionRate = totalOrders > 0
                ? round((double) completedOrders / totalOrders * 100, 1)
                : 0;

        // 商品好评率: 高评分商品占比（综合评分 >= 4.0）
        double positiveRate = totalApproved > 0
                ? round((double) highRatedCount / totalApproved * 100, 1)
                : 0;

        // 平台响应度: 平均评分转为百分制
        double responseSpeed = round(avgRating.doubleValue() / 5.0 * 100, 1);

        Map<String, Object> radar = new LinkedHashMap<>();
        radar.put("userActivity", userActivity);
        radar.put("productRichness", productRichness);
        radar.put("orderCompletionRate", orderCompletionRate);
        radar.put("positiveRate", positiveRate);
        radar.put("responseSpeed", responseSpeed);
        return radar;
    }

    /**
     * 安全地从 Map 值中提取 int，兼容 MySQL TINYINT(1) 被 JDBC 映射为 Boolean 的情况
     */
    private int toInt(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value ? 1 : 0;
        }
        return ((Number) value).intValue();
    }

    private double round(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }
}
