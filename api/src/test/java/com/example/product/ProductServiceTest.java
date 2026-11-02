package com.example.product;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.exception.BusinessException;
import com.example.mapper.*;
import com.example.pojo.dto.*;
import com.example.pojo.entity.*;
import com.example.service.impl.ProductServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ProductServiceTest {
    ProductMapper products; ProductImageMapper images; CategoryMapper categories; OrderMapper orders; ProductServiceImpl service;
    @BeforeEach void setup() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),""),Product.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),""),ProductImage.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),""),Order.class);
        products=mock(ProductMapper.class); images=mock(ProductImageMapper.class); categories=mock(CategoryMapper.class); orders=mock(OrderMapper.class);
        service=spy(new ProductServiceImpl(products,images,categories,orders));
    }
    Product product(int status) { return Product.builder().id(10).userId(2).categoryId(1).title("教材").description("").price(BigDecimal.TEN).condition(2).tradeType(1).stock(7).version(3).status(status).build(); }
    ProductUpdateDTO update() { ProductUpdateDTO d=new ProductUpdateDTO(); d.setId(10); d.setVersion(3); d.setTitle("新标题"); return d; }
    @Test void otherSellerCannotEdit() { doReturn(product(1)).when(service).getById(10); BusinessException e=assertThrows(BusinessException.class,()->service.update(3,update())); assertEquals(403,e.getCode()); verifyNoInteractions(images); }
    @Test void staleVersionCannotOverwrite() { doReturn(product(1)).when(service).getById(10); var d=update(); d.setVersion(2); assertThrows(BusinessException.class,()->service.update(2,d)); verifyNoInteractions(images); }
    @Test void contentEditPreservesStockAndRequiresAudit() { doReturn(product(1)).when(service).getById(10); doAnswer(invocation->{ Wrapper<Product> wrapper=invocation.getArgument(0); String sql=wrapper.getSqlSegment(); var update=(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Product>)wrapper; assertTrue(update.getSqlSet().contains("status=")); assertTrue(update.getParamNameValuePairs().containsValue(7)); assertTrue(update.getParamNameValuePairs().containsValue(0)); assertTrue(sql.contains("version")); return true; }).when(service).update(any(Wrapper.class)); service.update(2,update()); verifyNoInteractions(images); }
    @Test void downProductRemainsDownAfterEdit() { doReturn(product(3)).when(service).getById(10); doAnswer(invocation->{ var wrapper=(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Product>)invocation.getArgument(0); wrapper.getSqlSet(); assertTrue(wrapper.getParamNameValuePairs().containsValue(3)); return true; }).when(service).update(any(Wrapper.class)); service.update(2,update()); }
    @Test void conflictDoesNotReplaceImageRows() { doReturn(product(1)).when(service).getById(10); doReturn(false).when(service).update(any(Wrapper.class)); var d=update(); d.setImages(List.of(ProductImageDTO.builder().imageUrl("/files/a.png").build())); assertThrows(BusinessException.class,()->service.update(2,d)); verifyNoInteractions(images); }
    @Test void orderedProductCannotBeDeleted() { doReturn(product(1)).when(service).getById(10); when(orders.selectCount(any(Wrapper.class))).thenReturn(1L); var e=assertThrows(BusinessException.class,()->service.deleteById(2,10,false)); assertEquals(409,e.getCode()); verifyNoInteractions(images); }
    @Test void missingOrDisabledCategoryCannotBePublished() { var d=new ProductAddDTO(); d.setTitle("教材"); d.setCategoryId(1); d.setPrice(BigDecimal.ONE); d.setCondition(1); d.setTradeType(1); d.setImages(List.of(ProductImageDTO.builder().imageUrl("/files/a.png").build())); assertThrows(BusinessException.class,()->service.add(2,d)); verifyNoInteractions(images); }
}
