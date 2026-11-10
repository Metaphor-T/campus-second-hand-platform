package com.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticsMapper {
    Map<String, Object> selectTotals();
    Map<String, Object> selectPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<Map<String, Object>> selectDailyActivity(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<Map<String, Object>> selectCategories(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<Map<String, Object>> selectOrderStatuses(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    Map<String, Object> selectParticipation(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    Map<String, Object> selectReviews(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
