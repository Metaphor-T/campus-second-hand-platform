package com.example.product;

import com.example.controller.OrderOverviewController;
import com.example.exception.OrderOverviewExceptionHandler;
import com.example.interceptor.JwtInterceptor;
import com.example.mapper.*;
import com.example.pojo.vo.OrderVO;
import com.example.service.*;
import com.example.service.impl.OrderServiceImpl;
import com.example.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderOverviewHttpTest {
    OrderMapper mapper;JwtUtil jwt;MockMvc mvc;
    @BeforeEach void setup(){mapper=mock(OrderMapper.class);jwt=mock(JwtUtil.class);var orders=new OrderServiceImpl(mapper,mock(ProductService.class),mock(AddressMapper.class));mvc=MockMvcBuilders.standaloneSetup(new OrderOverviewController(new OrderOverviewService(orders))).setControllerAdvice(new OrderOverviewExceptionHandler()).addInterceptors(new JwtInterceptor(jwt,mock(SessionIdentityService.class))).build();when(mapper.getDetail(1)).thenReturn(OrderVO.builder().id(1).buyerId(2).sellerId(3).orderStatus(0).receiverPhone("13912345678").build());}
    void login(int id,String role){when(jwt.getClaims("test")).thenReturn(mock(Claims.class));when(jwt.getUserId("test")).thenReturn(String.valueOf(id));when(jwt.getRole("test")).thenReturn(role);}
    @Test void anonymousRejected()throws Exception{mvc.perform(get("/order/detail/1/overview")).andExpect(status().isUnauthorized());verifyNoInteractions(mapper);}
    @Test void adminCannotReadUserOverview()throws Exception{login(1,"ADMIN");mvc.perform(get("/order/detail/1/overview").header("Authorization","test")).andExpect(status().isForbidden());verifyNoInteractions(mapper);}
    @Test void buyerCanRead()throws Exception{login(2,"USER");mvc.perform(get("/order/detail/1/overview").header("Authorization","test")).andExpect(status().isOk()).andExpect(jsonPath("$.data.viewerRole").value("BUYER"));}
    @Test void sellerCanRead()throws Exception{login(3,"USER");mvc.perform(get("/order/detail/1/overview").header("Authorization","test")).andExpect(status().isOk()).andExpect(jsonPath("$.data.viewerRole").value("SELLER"));}
    @Test void thirdPartyCannotReadPrivateAddressByForgingUserId()throws Exception{login(4,"USER");mvc.perform(get("/order/detail/1/overview").param("userId","2").param("role","buyer").header("Authorization","test")).andExpect(status().isForbidden()).andExpect(jsonPath("$.data").doesNotExist()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("13912345678"))));}
    @Test void missingOrderIs404()throws Exception{login(2,"USER");mvc.perform(get("/order/detail/999/overview").header("Authorization","test")).andExpect(status().isNotFound());}
    @ParameterizedTest @ValueSource(strings={"0","-1","abc","2147483648"})
    void invalidIdIs400(String id)throws Exception{login(2,"USER");mvc.perform(get("/order/detail/"+id+"/overview").header("Authorization","test")).andExpect(status().isBadRequest());verifyNoInteractions(mapper);}
    @Test void databaseFailureDoesNotExposeSqlOrPrivateData()throws Exception{login(2,"USER");when(mapper.getDetail(1)).thenThrow(new RuntimeException("private SQL"));mvc.perform(get("/order/detail/1/overview").header("Authorization","test")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.message").value("订单详情暂时无法加载，请稍后重试")).andExpect(jsonPath("$.data").doesNotExist());}
}
