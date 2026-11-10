package com.example.statistics;

import com.example.mapper.StatisticsMapper;
import com.example.service.StatisticsService;
import com.example.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatisticsServiceTest {
    StatisticsMapper mapper; StatisticsService service;
    @BeforeEach void setup() {
        mapper=mock(StatisticsMapper.class); service=new StatisticsService(mapper);
        when(mapper.selectTotals()).thenReturn(Map.of()); when(mapper.selectPeriod(any(),any())).thenReturn(Map.of());
        when(mapper.selectDailyActivity(any(),any())).thenReturn(List.of()); when(mapper.selectCategories(any(),any())).thenReturn(List.of());
        when(mapper.selectOrderStatuses(any(),any())).thenReturn(List.of());
        when(mapper.selectParticipation(any(),any())).thenReturn(Map.of()); when(mapper.selectReviews(any(),any())).thenReturn(Map.of());
    }
    @Test void zeroSampleIsNullAndMissingDaysAreRetained() {
        var result=service.getDashboard("2024-02-28","2024-03-01");
        assertEquals(3,result.trends().size()); assertEquals("2024-02-29",result.trends().get(1).date());
        assertTrue(result.trends().stream().allMatch(d -> d.users()==0&&d.products()==0&&d.orders()==0));
        assertTrue(result.ratios().stream().allMatch(r -> r.percent()==null));
        assertEquals(6,result.orderStatus().size()); assertTrue(result.orderStatus().stream().allMatch(s -> s.share()==null));
    }
    @Test void ratiosUseExplicitMatchingDenominatorsAndDecimalAmounts() {
        when(mapper.selectPeriod(any(),any())).thenReturn(Map.of("users",2L,"products",4L,"orders",3L,"completedOrders",1L,"completedQuantity",3L,"completedAmount",new BigDecimal("31.25")));
        when(mapper.selectDailyActivity(any(),any())).thenReturn(List.of(Map.of("date","2024-02-29","users",2,"products",4,"orders",3)));
        when(mapper.selectCategories(any(),any())).thenReturn(List.of(Map.of("id",1,"name","图书","products",4,"completedOrders",1,"completedQuantity",3,"completedAmount",new BigDecimal("31.25"))));
        when(mapper.selectOrderStatuses(any(),any())).thenReturn(List.of(Map.of("status",3,"count",1),Map.of("status",99,"count",2)));
        when(mapper.selectParticipation(any(),any())).thenReturn(Map.of("eligibleUsers",3L,"buyers",1L,"publishers",2L));
        when(mapper.selectReviews(any(),any())).thenReturn(Map.of("visibleReviews",2L,"positiveReviews",1L,"reviewedCompletedOrders",1L));
        var result=service.getDashboard("2024-02-28","2024-03-01");
        assertEquals(new BigDecimal("31.25"),result.period().completedAmount());
        assertEquals(new BigDecimal("100.0"),result.categories().getFirst().quantityShare());
        assertEquals(new BigDecimal("33.3"),result.ratios().get(0).percent());
        assertEquals(new BigDecimal("66.7"),result.ratios().get(1).percent());
        assertEquals(new BigDecimal("50.0"),result.ratios().get(3).percent());
        assertEquals(new BigDecimal("100.0"),result.ratios().get(4).percent());
        assertEquals(3,result.orderStatus().stream().mapToLong(s -> s.count()).sum());
        assertEquals("未知状态",result.orderStatus().getLast().name());
        assertEquals(2,result.trends().get(1).users());
    }
    @Test void invalidDateDoesNotQueryDatabase() {
        clearInvocations(mapper);
        assertThrows(BusinessException.class,() -> service.getDashboard("2024-03-02","2024-03-01"));
        verifyNoInteractions(mapper);
    }
    @Test void todayBadgesUseBeijingDayAndDoNotFollowHistoricalRange() {
        var today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        when(mapper.selectPeriod(today.atStartOfDay(),today.plusDays(1).atStartOfDay()))
            .thenReturn(Map.of("users",2L,"products",3L,"orders",4L));
        var first=service.getDashboard("2024-02-28","2024-03-01");
        var second=service.getDashboard("2024-06-01","2024-06-30");
        assertEquals(2,first.overview().todayUsers()); assertEquals(3,first.overview().todayProducts()); assertEquals(4,first.overview().todayOrders());
        assertEquals(first.overview(),second.overview());
        assertEquals(0,first.period().orders()); assertEquals(0,second.period().orders());
        verify(mapper,times(2)).selectPeriod(today.atStartOfDay(),today.plusDays(1).atStartOfDay());
    }
}
