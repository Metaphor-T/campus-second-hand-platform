package com.example.controller;

import com.example.annotation.Auth;
import com.example.pojo.api.Result;
import com.example.pojo.dto.*;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.AfterSaleContextVO;
import com.example.service.OrderAfterSaleService;
import com.example.utils.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order/after-sale")
@RequiredArgsConstructor
public class OrderAfterSaleController {
    private final OrderAfterSaleService service;

    @Auth({RoleEnum.USER,RoleEnum.ADMIN})
    @GetMapping("/order/{orderId}")
    public Result<AfterSaleContextVO> read(@PathVariable Integer orderId) {
        return Result.success(service.read(orderId,UserContext.getUserId(),UserContext.getRole()));
    }
    @Auth({RoleEnum.USER})
    @PostMapping("/order/{orderId}")
    public Result<Void> apply(@PathVariable Integer orderId,@Valid @RequestBody AfterSaleApplyDTO dto) {
        service.apply(UserContext.getUserId(),orderId,dto);return Result.success();
    }
    @Auth({RoleEnum.USER})
    @PutMapping("/{id}/seller-response")
    public Result<Void> respond(@PathVariable Integer id,@Valid @RequestBody AfterSaleResponseDTO dto) {
        service.sellerRespond(UserContext.getUserId(),id,dto);return Result.success();
    }
    @Auth({RoleEnum.ADMIN})
    @PutMapping("/{id}/admin-review")
    public Result<Void> review(@PathVariable Integer id,@Valid @RequestBody AfterSaleReviewDTO dto) {
        service.review(UserContext.getUserId(),id,dto);return Result.success();
    }
    @Auth({RoleEnum.USER,RoleEnum.ADMIN})
    @PutMapping("/{id}/confirm-return")
    public Result<Void> confirm(@PathVariable Integer id,@Valid @RequestBody AfterSaleReturnDTO dto) {
        service.confirmReturn(UserContext.getUserId(),UserContext.getRole(),id,dto);return Result.success();
    }
    @Auth({RoleEnum.USER})
    @PutMapping("/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable Integer id) {
        service.withdraw(UserContext.getUserId(),id);return Result.success();
    }
}
