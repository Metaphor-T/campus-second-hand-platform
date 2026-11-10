package com.example.service;

import com.example.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;

/** 包含首尾自然日；SQL 使用 [start, endExclusive) 避免漏掉结束日的秒/微秒。 */
public record StatisticsRange(LocalDate startDate, LocalDate endDate) {
    public static StatisticsRange resolve(String start, String end, LocalDate today) {
        if (start == null && end == null) return new StatisticsRange(today.minusDays(29), today);
        if (start == null || end == null) throw new BusinessException("请同时提供开始日期和结束日期", 400);
        try {
            LocalDate from = LocalDate.parse(start), to = LocalDate.parse(end);
            if (from.isBefore(LocalDate.of(1000, 1, 1)) || from.isAfter(to))
                throw new BusinessException("开始日期不能晚于结束日期，且不能早于 1000-01-01", 400);
            if (to.isAfter(today)) throw new BusinessException("结束日期不能晚于今天（北京时间）", 400);
            if (ChronoUnit.DAYS.between(from, to) >= 366)
                throw new BusinessException("每次最多查询 366 个自然日", 400);
            return new StatisticsRange(from, to);
        } catch (DateTimeParseException e) {
            throw new BusinessException("日期格式应为 yyyy-MM-dd，且必须是有效日期", 400);
        }
    }
    public LocalDateTime start() { return startDate.atStartOfDay(); }
    public LocalDateTime endExclusive() { return endDate.plusDays(1).atStartOfDay(); }
}
