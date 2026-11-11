package com.example.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_after_sale")
public class OrderAfterSale {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer orderId;
    private String requestType;
    private String state;
    private String reason;
    private String evidenceJson;
    private Integer originalOrderStatus;
    private Boolean returnRequired;
    private Integer applicantId;
    private Boolean sellerAgree;
    private String sellerNote;
    private Integer sellerId;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private LocalDateTime sellerRespondedAt;
    private Integer adminId;
    private String adminNote;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private LocalDateTime adminReviewedAt;
    private Integer returnConfirmedBy;
    private String returnNote;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private LocalDateTime returnConfirmedAt;
    private Integer returnQuantity;
    private Integer stockCreditedQuantity;
    private String stockDisposition;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private LocalDateTime createdAt;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private LocalDateTime updatedAt;
}
