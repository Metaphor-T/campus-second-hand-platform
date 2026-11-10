package com.example.statistics;

import com.example.controller.StatisticsController;
import com.example.exception.StatisticsExceptionHandler;
import com.example.interceptor.JwtInterceptor;
import com.example.mapper.StatisticsMapper;
import com.example.service.StatisticsService;
import com.example.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StatisticsHttpTest {
    StatisticsMapper mapper; JwtUtil jwt; MockMvc mvc;
    @BeforeEach void setup() {
        mapper=mock(StatisticsMapper.class); jwt=mock(JwtUtil.class);
        mvc=MockMvcBuilders.standaloneSetup(new StatisticsController(new StatisticsService(mapper)))
            .setControllerAdvice(new StatisticsExceptionHandler()).addInterceptors(new JwtInterceptor(jwt)).build();
    }
    void role(String role) { when(jwt.getClaims("test")).thenReturn(mock(Claims.class)); when(jwt.getUserId("test")).thenReturn("1"); when(jwt.getRole("test")).thenReturn(role); }
    @Test void guestCannotReadStatistics() throws Exception { mvc.perform(get("/stats/dashboard")).andExpect(status().isUnauthorized()); verifyNoInteractions(mapper); }
    @Test void ordinaryUserCannotReadStatistics() throws Exception { role("USER"); mvc.perform(get("/stats/dashboard").header("Authorization","Bearer test")).andExpect(status().isForbidden()); verifyNoInteractions(mapper); }
    @Test void administratorInvalidDateIs400() throws Exception { role("ADMIN"); mvc.perform(get("/stats/dashboard").header("Authorization","Bearer test").param("startDate","2026-02-30").param("endDate","2026-03-01")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400)); verifyNoInteractions(mapper); }
    @Test void queryFailureDoesNotReturnFakeZeroOrSqlDetails() throws Exception { role("ADMIN"); when(mapper.selectTotals()).thenThrow(new RuntimeException("private SQL error")); mvc.perform(get("/stats/dashboard").header("Authorization","Bearer test")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.data").doesNotExist()).andExpect(jsonPath("$.message").value("统计数据暂时无法加载，请稍后重试")); }
}
