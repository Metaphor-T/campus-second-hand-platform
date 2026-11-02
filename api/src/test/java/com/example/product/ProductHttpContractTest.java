package com.example.product;

import com.example.controller.*;
import com.example.exception.ProductExceptionHandler;
import com.example.interceptor.JwtInterceptor;
import com.example.pojo.vo.ProductVO;
import com.example.service.*;
import com.example.utils.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductHttpContractTest {
    ProductService products; MockMvc mvc;
    @BeforeEach void setup() {
        products=mock(ProductService.class);
        mvc=MockMvcBuilders.standaloneSetup(new GuestProductController(products,mock(CategoryService.class)),new ProductController(products,mock(UserInteractionService.class)),new ProductImageUploadController(mock(ProductImageStorageService.class)))
                .setControllerAdvice(new ProductExceptionHandler()).addInterceptors(new JwtInterceptor(mock(JwtUtil.class))).build();
    }
    @Test void guestCanViewApprovedDetailWithoutToken() throws Exception { when(products.getDetail(1)).thenReturn(ProductVO.builder().id(1).status(1).build()); mvc.perform(get("/guest/product/detail/1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1)); }
    @Test void guestCannotViewPendingDetail() throws Exception { when(products.getDetail(1)).thenReturn(ProductVO.builder().id(1).status(0).build()); mvc.perform(get("/guest/product/detail/1")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404)); }
    @Test void guestInvalidPriceRangeIs400() throws Exception { mvc.perform(post("/guest/product/market").contentType(MediaType.APPLICATION_JSON).content("{\"minPrice\":10,\"maxPrice\":1}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400)); verifyNoInteractions(products); }
    @Test void malformedJsonIs400() throws Exception { mvc.perform(post("/guest/product/market").contentType(MediaType.APPLICATION_JSON).content("{bad")).andExpect(status().isBadRequest()); }
    @Test void productUploadRequiresLogin() throws Exception { mvc.perform(multipart("/product/images/upload").file("file",new byte[]{1})).andExpect(status().isUnauthorized()); }
}
