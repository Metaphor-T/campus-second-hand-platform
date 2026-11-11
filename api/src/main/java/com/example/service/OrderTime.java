package com.example.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** 订单节点与售后期限统一采用北京时间，匹配数据库 DATETIME 的秒级精度。 */
public final class OrderTime {
    private OrderTime() {}
    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai")).withNano(0);
    }
}
