package com.example.service;

import com.example.mapper.StatisticsMapper;
import com.example.pojo.vo.StatisticsDashboardVO;
import com.example.pojo.vo.StatisticsDashboardVO.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<String> STATUS_NAMES = List.of("待付款", "待发货", "待收货", "已完成", "已结束", "退款/售后");
    private final StatisticsMapper statisticsMapper;

    /** 一次请求、一个一致性只读快照，避免各图分别请求时总数互相对不上。 */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public StatisticsDashboardVO getDashboard(String startDate, String endDate) {
        var now = ZonedDateTime.now(ZONE);
        var range = StatisticsRange.resolve(startDate, endDate, now.toLocalDate());
        var start = range.start(); var end = range.endExclusive();
        var total = statisticsMapper.selectTotals();
        var p = statisticsMapper.selectPeriod(start, end);
        // 今日角标始终使用北京时间的自然日，不能从用户选择的统计期间取值。
        var todayStart = now.toLocalDate().atStartOfDay();
        var todayCounts = statisticsMapper.selectPeriod(todayStart, todayStart.plusDays(1));
        var overview = new Overview(n(total,"users"),n(total,"products"),n(total,"orders"),money(total,"completedAmount"),
            n(total,"pendingProducts"),n(total,"refundOrders"),n(todayCounts,"users"),n(todayCounts,"products"),n(todayCounts,"orders"));
        var period = new Period(n(p,"users"),n(p,"products"),n(p,"orders"),n(p,"completedOrders"),n(p,"completedQuantity"),money(p,"completedAmount"));
        Map<String, Map<String,Object>> daily = new HashMap<>();
        for (var row : statisticsMapper.selectDailyActivity(start,end)) daily.put(row.get("date").toString(), row);
        List<Daily> trends = new ArrayList<>();
        for (LocalDate date=range.startDate(); !date.isAfter(range.endDate()); date=date.plusDays(1)) {
            var row=daily.getOrDefault(date.toString(),Map.of());
            trends.add(new Daily(date.toString(),n(row,"users"),n(row,"products"),n(row,"orders")));
        }
        List<Category> categories = statisticsMapper.selectCategories(start,end).stream().map(row ->
            new Category(n(row,"id"),row.get("name").toString(),n(row,"products"),percent(n(row,"products"),period.products()),
                n(row,"completedOrders"),n(row,"completedQuantity"),percent(n(row,"completedQuantity"),period.completedQuantity()),money(row,"completedAmount"))).toList();
        Map<Integer,Long> counts = new TreeMap<>();
        for (var row : statisticsMapper.selectOrderStatuses(start,end)) counts.put((int)n(row,"status"),n(row,"count"));
        List<OrderStatus> statuses = new ArrayList<>();
        for (int i=0;i<STATUS_NAMES.size();i++) statuses.add(new OrderStatus(i,STATUS_NAMES.get(i),counts.getOrDefault(i,0L),percent(counts.getOrDefault(i,0L),period.orders())));
        long unknown=counts.entrySet().stream().filter(e -> e.getKey()<0 || e.getKey()>=STATUS_NAMES.size()).mapToLong(Map.Entry::getValue).sum();
        if (unknown>0) statuses.add(new OrderStatus(-1,"未知状态",unknown,percent(unknown,period.orders())));
        var participation=statisticsMapper.selectParticipation(start,end);
        var reviews=statisticsMapper.selectReviews(start,end);
        long eligible=n(participation,"eligibleUsers");
        List<Ratio> ratios=List.of(
            ratio("buyers","下单用户占比",n(participation,"buyers"),eligible,"期间下过单的普通用户（去重，含后续取消）÷ 截至结束日期已注册的普通用户"),
            ratio("publishers","发布用户占比",n(participation,"publishers"),eligible,"期间发布过商品的普通用户（去重，含各审核状态）÷ 截至结束日期已注册的普通用户"),
            ratio("completion","订单完成占比",period.completedOrders(),period.orders(),"期间新建且当前已完成、无处理中售后的订单 ÷ 期间全部新建订单"),
            ratio("positiveReviews","可见评价好评占比",n(reviews,"positiveReviews"),n(reviews,"visibleReviews"),"期间新增的可见 4—5 星评价 ÷ 期间新增的可见有效评价（1—5 星）；隐藏评价不计入"),
            ratio("reviewCoverage","完成订单评价覆盖率",n(reviews,"reviewedCompletedOrders"),period.completedOrders(),"当前已有可见有效评价且无处理中售后的期间已完成订单（按订单去重）÷ 期间已完成订单；评价时间不限")
        );
        return new StatisticsDashboardVO(now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),ZONE.getId(),now.toLocalDate().toString(),
            range.startDate().toString(),range.endDate().toString(),overview,period,trends,categories,statuses,ratios);
    }
    private static long n(Map<String,Object> row,String key) { return row.get(key) instanceof Number value ? value.longValue() : 0; }
    private static BigDecimal money(Map<String,Object> row,String key) { return new BigDecimal(row.getOrDefault(key,BigDecimal.ZERO).toString()).setScale(2,RoundingMode.HALF_UP); }
    private static BigDecimal percent(long numerator,long denominator) {
        return denominator==0 ? null : BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominator),1,RoundingMode.HALF_UP);
    }
    private static Ratio ratio(String key,String name,long numerator,long denominator,String definition) {
        return new Ratio(key,name,numerator,denominator,percent(numerator,denominator),definition);
    }
}
