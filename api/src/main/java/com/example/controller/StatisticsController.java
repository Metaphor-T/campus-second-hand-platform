package com.example.controller;

import com.example.annotation.Auth;
import com.example.pojo.api.Result;
import com.example.pojo.enums.RoleEnum;
import com.example.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统计分析控制器 - 管理员仪表盘数据
 */
@RestController
@RequestMapping(value = "/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 概览统计卡片（用户总数、商品总数、订单总数、成交额、待审核、待退款、今日新增等）
     */
    @Auth({RoleEnum.ADMIN})
    @GetMapping(value = "/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = statisticsService.getOverview();
        return Result.success(data);
    }

    /**
     * 近N天新增趋势（折线图）
     *
     * @param days 天数，默认7天
     */
    @Auth({RoleEnum.ADMIN})
    @GetMapping(value = "/trends")
    public Result<Map<String, Object>> trends(@RequestParam(value = "days", defaultValue = "7") Integer days) {
        Map<String, Object> data = statisticsService.getTrends(days);
        return Result.success(data);
    }

    /**
     * 分类销量排行（柱状图）
     */
    @Auth({RoleEnum.ADMIN})
    @GetMapping(value = "/category-sales")
    public Result<List<Map<String, Object>>> categorySales() {
        List<Map<String, Object>> data = statisticsService.getCategorySales();
        return Result.success(data);
    }

    /**
     * 订单状态分布（饼状图）
     */
    @Auth({RoleEnum.ADMIN})
    @GetMapping(value = "/order-status")
    public Result<List<Map<String, Object>>> orderStatus() {
        List<Map<String, Object>> data = statisticsService.getOrderStatusDistribution();
        return Result.success(data);
    }

    /**
     * 平台健康综合指标（雷达图）
     */
    @Auth({RoleEnum.ADMIN})
    @GetMapping(value = "/radar")
    public Result<Map<String, Object>> radar() {
        Map<String, Object> data = statisticsService.getRadarData();
        return Result.success(data);
    }
}
