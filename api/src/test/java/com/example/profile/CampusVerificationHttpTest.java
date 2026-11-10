package com.example.profile;

import com.example.controller.UserProfileController;
import com.example.exception.UserProfileExceptionHandler;
import com.example.interceptor.JwtInterceptor;
import com.example.service.*;
import com.example.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CampusVerificationHttpTest {
    CampusVerificationService service;JwtUtil jwt;MockMvc mvc;
    @BeforeEach void setup() {
        service=mock(CampusVerificationService.class);jwt=mock(JwtUtil.class);
        mvc=MockMvcBuilders.standaloneSetup(new UserProfileController(mock(UserProfileService.class),service))
            .setControllerAdvice(new UserProfileExceptionHandler()).addInterceptors(new JwtInterceptor(jwt,mock(com.example.service.SessionIdentityService.class))).build();
    }
    void role(String role) { when(jwt.getClaims("token")).thenReturn(mock(Claims.class));when(jwt.getUserId("token")).thenReturn("2");when(jwt.getRole("token")).thenReturn(role); }
    @Test void allEndpointsRequireLogin() throws Exception {
        mvc.perform(get("/user/profile/campus-verification")).andExpect(status().isUnauthorized());
        mvc.perform(post("/user/profile/campus-verification")).andExpect(status().isUnauthorized());
        mvc.perform(post("/user/profile/campus-verification/refresh")).andExpect(status().isUnauthorized());verifyNoInteractions(service);
    }
    @Test void adminCannotCertifyOnBehalfOfUser() throws Exception { role("ADMIN");mvc.perform(post("/user/profile/campus-verification").header("Authorization","token").contentType(MediaType.APPLICATION_JSON).content("{\"consent\":true}")).andExpect(status().isForbidden());verifyNoInteractions(service); }
    @ParameterizedTest @ValueSource(strings={"{}","{\"consent\":false}","{\"consent\":null}"})
    void explicitConsentRequired(String body) throws Exception { role("USER");mvc.perform(post("/user/profile/campus-verification").header("Authorization","token").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());verifyNoInteractions(service); }
    @Test void clientCannotSupplyIdentityOrTrustedResult() throws Exception { role("USER");mvc.perform(post("/user/profile/campus-verification").header("Authorization","token").contentType(MediaType.APPLICATION_JSON).content("{\"consent\":true,\"id\":1,\"school\":\"伪造\",\"status\":\"VERIFIED\"}")).andExpect(status().isOk());verify(service).apply();verifyNoMoreInteractions(service); }
    @Test void refreshOnlyTriggersServerLookup() throws Exception { role("USER");mvc.perform(post("/user/profile/campus-verification/refresh").header("Authorization","token").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VERIFIED\"}")).andExpect(status().isOk());verify(service).refresh(); }
    @Test void defaultUnavailableStatusUses503WithoutChangingRecord() throws Exception { role("USER");when(service.apply()).thenThrow(new com.example.exception.BusinessException("暂未接入",503));mvc.perform(post("/user/profile/campus-verification").header("Authorization","token").contentType(MediaType.APPLICATION_JSON).content("{\"consent\":true}")).andExpect(status().isServiceUnavailable()); }
}
