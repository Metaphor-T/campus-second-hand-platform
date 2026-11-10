package com.example.pojo.vo;

import java.math.BigDecimal;
import java.util.List;

/** 同一只读快照中的仪表盘数据。期间订单按下单时间归组，状态取查询时当前值。 */
public record StatisticsDashboardVO(
        String generatedAt, String timezone, String today, String startDate, String endDate,
        Overview overview, Period period, List<Daily> trends, List<Category> categories,
        List<OrderStatus> orderStatus, List<Ratio> ratios) {
    public record Overview(long users, long products, long orders, BigDecimal completedAmount,
                           long pendingProducts, long refundOrders,
                           long todayUsers, long todayProducts, long todayOrders) {}
    public record Period(long users, long products, long orders, long completedOrders,
                         long completedQuantity, BigDecimal completedAmount) {}
    public record Daily(String date, long users, long products, long orders) {}
    public record Category(long id, String name, long products, BigDecimal productShare,
                           long completedOrders, long completedQuantity, BigDecimal quantityShare,
                           BigDecimal completedAmount) {}
    public record OrderStatus(int status, String name, long count, BigDecimal share) {}
    /** 分母为零时 percent 为 null，前端展示“暂无样本”。 */
    public record Ratio(String key, String name, long numerator, long denominator,
                        BigDecimal percent, String definition) {}
}
