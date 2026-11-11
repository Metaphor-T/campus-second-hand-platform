package com.example.service;

import java.time.LocalDateTime;
import java.util.Set;

public final class AfterSalePolicy {
    private AfterSalePolicy() {}
    public static final Set<String> ACTIVE = Set.of("WAIT_SELLER","WAIT_ADMIN","WAIT_RETURN");
    public static boolean regular(Integer orderStatus, LocalDateTime received, LocalDateTime deadline, LocalDateTime now) {
        if (Integer.valueOf(2).equals(orderStatus)) return true;
        return Integer.valueOf(3).equals(orderStatus) && received != null && !received.isAfter(now)
                && deadline != null && now.isBefore(deadline);
    }
    public static String label(String state) {
        if (state == null) return "未申请售后";
        return switch(state) {
            case "WAIT_SELLER" -> "等待卖家回应";
            case "WAIT_ADMIN" -> "等待管理员审核";
            case "WAIT_RETURN" -> "审核通过，等待退回商品";
            case "APPROVED" -> "售后已完成";
            case "REJECTED" -> "售后申请已驳回";
            case "WITHDRAWN" -> "售后申请已撤回";
            default -> "历史售后待核对";
        };
    }
}
