package com.example.controller;

import com.example.annotation.Auth;
import com.example.pojo.api.Result;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.StatisticsDashboardVO;
import com.example.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService statisticsService;

    /** 替代旧的五个独立图表请求，让累计、期间、明细在同一数据库快照中返回。 */
    @Auth({RoleEnum.ADMIN})
    @GetMapping("/dashboard")
    public Result<StatisticsDashboardVO> dashboard(@RequestParam(required=false) String startDate,
                                                 @RequestParam(required=false) String endDate) {
        return Result.success(statisticsService.getDashboard(startDate,endDate));
    }
}
