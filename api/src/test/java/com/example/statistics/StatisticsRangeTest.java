package com.example.statistics;

import com.example.exception.BusinessException;
import com.example.service.StatisticsRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class StatisticsRangeTest {
    final LocalDate today=LocalDate.of(2026,11,10);
    @Test void defaultContainsThirtyNaturalDays() {
        var range=StatisticsRange.resolve(null,null,today);
        assertEquals(today.minusDays(29),range.startDate()); assertEquals(today,range.endDate());
    }
    @Test void includesLeapDayAndUsesExclusiveEnd() {
        var range=StatisticsRange.resolve("2024-02-28","2024-03-01",today);
        assertEquals("2024-02-28T00:00",range.start().toString());
        assertEquals("2024-03-02T00:00",range.endExclusive().toString());
    }
    @Test void exactly366DaysAllowed() { assertDoesNotThrow(() -> StatisticsRange.resolve("2024-01-01","2024-12-31",today)); }
    @Test void onlyOneDateRejected() { assertThrows(BusinessException.class,() -> StatisticsRange.resolve(null,"2026-11-10",today)); }
    @ParameterizedTest @CsvSource({"2026-11-11,2026-11-11", "2026-11-10,2026-11-09", "2024-01-01,2025-01-01", "2025-02-29,2025-03-01", "invalid,2026-11-10", "0999-01-01,0999-01-02"})
    void invalidRangesRejected(String start,String end) {
        assertEquals(400,assertThrows(BusinessException.class,() -> StatisticsRange.resolve(start,end,today)).getCode());
    }
}
