package com.example.controller;

import com.example.annotation.Auth;
import com.example.pojo.api.Result;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.OrderOverviewVO;
import com.example.service.OrderOverviewService;
import com.example.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/order/detail") @Auth(RoleEnum.USER) @RequiredArgsConstructor
public class OrderOverviewController {
    private final OrderOverviewService overview;
    @GetMapping("/{orderId}/overview")
    public Result<OrderOverviewVO> read(@PathVariable Integer orderId){return Result.success(overview.read(orderId,UserContext.getUserId()));}
}
