package com.example.pojo.vo;

import com.example.pojo.entity.OrderAfterSale;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public record AfterSaleContextVO(Integer orderId, Integer orderStatus, Integer quantity, String viewerRole, String eligibility,
        String eligibilityMessage,
        @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8") LocalDateTime deadline,
        @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8") LocalDateTime serverTime,
        List<OrderAfterSale> requests) {}
