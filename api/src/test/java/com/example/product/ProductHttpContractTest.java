package com.example.product;

import com.example.controller.*;
import com.example.exception.ProductExceptionHandler;
import com.example.interceptor.JwtInterceptor;
import com.example.pojo.vo.ProductVO;
import com.example.service.*;
import com.example.utils.JwtUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import io.jsonwebtoken.Claims;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductHttpContractTest {
    ProductService products; MockMvc mvc; JwtUtil jwt;
    @BeforeEach void setup() {
        products=mock(ProductService.class);
        jwt=mock(JwtUtil.class);
        mvc=MockMvcBuilders.standaloneSetup(new GuestProductController(products,mock(CategoryService.class)),new ProductController(products,mock(UserInteractionService.class)),new ProductImageUploadController(mock(ProductImageStorageService.class)))
                .setControllerAdvice(new ProductExceptionHandler()).addInterceptors(new JwtInterceptor(jwt,mock(com.example.service.SessionIdentityService.class))).build();
    }
    @Test void guestCanViewApprovedDetailWithoutToken() throws Exception { when(products.getDetail(1)).thenReturn(ProductVO.builder().id(1).status(1).build()); mvc.perform(get("/guest/product/detail/1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1)); }
    @Test void guestCannotViewPendingDetail() throws Exception { when(products.getDetail(1)).thenReturn(ProductVO.builder().id(1).status(0).build()); mvc.perform(get("/guest/product/detail/1")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404)); }
    @Test void guestInvalidPriceRangeIs400() throws Exception { mvc.perform(post("/guest/product/market").contentType(MediaType.APPLICATION_JSON).content("{\"minPrice\":10,\"maxPrice\":1}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400)); verifyNoInteractions(products); }
    @Test void malformedJsonIs400() throws Exception { mvc.perform(post("/guest/product/market").contentType(MediaType.APPLICATION_JSON).content("{bad")).andExpect(status().isBadRequest()); }
    @Test void productUploadRequiresLogin() throws Exception { mvc.perform(multipart("/product/images/upload").file("file",new byte[]{1})).andExpect(status().isUnauthorized()); }

    private void authenticateSeller() {
        when(jwt.getClaims("seller-token")).thenReturn(mock(Claims.class));
        when(jwt.getUserId("seller-token")).thenReturn("2");
        when(jwt.getRole("seller-token")).thenReturn("USER");
    }

    private String publishJson(String price) {
        return """
                {"title":"教材","categoryId":1,"price":%s,"condition":2,"tradeType":1,
                 "stock":0,"stockMode":1,"images":[{"imageUrl":"/files/a.png","isCover":1,"sort":0}]}
                """.formatted(price);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.001", "100000000.00"})
    void authenticatedPublishRejectsInvalidPriceBeforeCallingService(String price) throws Exception {
        authenticateSeller();
        mvc.perform(post("/product/add").header("Authorization", "Bearer seller-token")
                        .contentType(MediaType.APPLICATION_JSON).content(publishJson(price)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(products);
    }

    @Test void authenticatedPublishAcceptsValidDtoAndPassesCurrentUser() throws Exception {
        authenticateSeller();
        mvc.perform(post("/product/add").header("Authorization", "Bearer seller-token")
                        .contentType(MediaType.APPLICATION_JSON).content(publishJson("20")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(products).add(eq(2), argThat(dto -> dto.getTitle().equals("教材")
                && dto.getPrice().intValueExact() == 20 && dto.getImages().size() == 1));
    }

    @Test void authenticatedPublishRejectsInvalidNestedImage() throws Exception {
        authenticateSeller();
        mvc.perform(post("/product/add").header("Authorization", "Bearer seller-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishJson("20").replace("/files/a.png", "")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("图片地址不能为空"));
        verifyNoInteractions(products);
    }

    @Test void authenticatedPartialEditAllowsOmittedFields() throws Exception {
        authenticateSeller();
        mvc.perform(put("/product/update").header("Authorization", "Bearer seller-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"id\":10,\"description\":\"补充说明\"}"))
                .andExpect(status().isOk());
        verify(products).update(eq(2), argThat(dto -> dto.getId() == 10
                && dto.getPrice() == null && dto.getImages() == null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"description\":\"说明\"}", "{\"id\":0}", "{\"id\":10,\"version\":-1}",
            "{\"id\":10,\"title\":\"   \"}", "{\"id\":10,\"images\":[]}"})
    void authenticatedEditRejectsInvalidDtoBeforeCallingService(String body) throws Exception {
        authenticateSeller();
        mvc.perform(put("/product/update").header("Authorization", "Bearer seller-token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(products);
    }

    @ParameterizedTest
    @CsvSource({"0,12", "100001,12", "1,0", "1,101"})
    void guestPaginationIsRejectedBeforeCallingService(int page, int size) throws Exception {
        mvc.perform(post("/guest/product/market").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current\":%d,\"size\":%d}".formatted(page, size)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(products);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"sortBy\":\"unknown\"}", "{\"minPrice\":-1}", "{\"condition\":5}",
            "{\"tradeType\":0}", "{\"categoryId\":0}", "{\"status\":4}"})
    void guestInvalidFiltersCannotReachQueryService(String body) throws Exception {
        mvc.perform(post("/guest/product/market").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(products);
    }
}
