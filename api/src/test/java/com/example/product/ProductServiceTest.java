package com.example.product;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.exception.BusinessException;
import com.example.mapper.*;
import com.example.pojo.dto.*;
import com.example.pojo.entity.*;
import com.example.pojo.entity.Order;
import com.example.service.impl.ProductServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ProductServiceTest {
    private static ValidatorFactory factory;
    private ProductMapper products;
    private ProductImageMapper images;
    private CategoryMapper categories;
    private OrderMapper orders;
    private ProductServiceImpl service;

    @BeforeAll static void setupValidator() { factory = Validation.buildDefaultValidatorFactory(); }
    @AfterAll static void closeValidator() { factory.close(); }

    @BeforeEach void setup() {
        for (Class<?> entity : List.of(Product.class, ProductImage.class, Order.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entity);
        }
        products = mock(ProductMapper.class);
        images = mock(ProductImageMapper.class);
        categories = mock(CategoryMapper.class);
        orders = mock(OrderMapper.class);
        service = spy(new ProductServiceImpl(products, images, categories, orders, factory.getValidator()));
    }

    private Product product(int status) {
        return Product.builder().id(10).userId(2).categoryId(1).title("教材").description("")
                .price(BigDecimal.TEN).condition(2).tradeType(1).stock(7).version(3).status(status)
                .auditRemark("旧备注").auditTime(LocalDateTime.now()).auditorId(1).build();
    }

    private ProductUpdateDTO update() {
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setId(10);
        dto.setVersion(3);
        dto.setTitle("  新标题  ");
        return dto;
    }

    private ProductImageDTO image(String path, int cover) {
        return ProductImageDTO.builder().imageUrl(path).isCover(cover).sort(99).build();
    }

    private ProductAddDTO publish(List<ProductImageDTO> album) {
        ProductAddDTO dto = new ProductAddDTO();
        dto.setTitle("  教材  ");
        dto.setCategoryId(1);
        dto.setPrice(BigDecimal.TEN);
        dto.setCondition(2);
        dto.setTradeType(1);
        dto.setImages(album);
        return dto;
    }

    private void enabledCategory() {
        Category category = new Category();
        category.setId(1);
        category.setStatus(1);
        when(categories.selectById(1)).thenReturn(category);
    }

    private void successfulSave() {
        doAnswer(call -> {
            Product saved = call.getArgument(0);
            saved.setId(10);
            return true;
        }).when(service).save(any(Product.class));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private LambdaUpdateWrapper<Product> savedUpdate() {
        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(service).update(captor.capture());
        return (LambdaUpdateWrapper<Product>) captor.getValue();
    }

    private Object boundValue(LambdaUpdateWrapper<Product> wrapper, String sql, String field) {
        var match = Pattern.compile("(?<![\\w])`?" + Pattern.quote(field)
                + "`?\\s*=\\s*#\\{[^}]*\\.([^}.]+)}").matcher(sql);
        assertTrue(match.find(), () -> "缺少字段条件/赋值: " + field + "，SQL: " + sql);
        assertTrue(wrapper.getParamNameValuePairs().containsKey(match.group(1)));
        return wrapper.getParamNameValuePairs().get(match.group(1));
    }

    private void assertRejectedAlbum(String operation, List<ProductImageDTO> album, String message) {
        BusinessException error;
        if (operation.equals("publish")) {
            enabledCategory();
            error = assertThrows(BusinessException.class, () -> service.add(2, publish(album)));
        } else {
            doReturn(product(1)).when(service).getById(10);
            ProductUpdateDTO dto = update();
            dto.setImages(album);
            error = assertThrows(BusinessException.class, () -> service.update(2, dto));
        }
        assertEquals(400, error.getCode());
        assertEquals(message, error.getMessage());
        verify(service, never()).save(any(Product.class));
        verify(service, never()).update(any(Wrapper.class));
        verifyNoInteractions(images);
    }

    @Test void otherSellerCannotEdit() {
        doReturn(product(1)).when(service).getById(10);
        BusinessException error = assertThrows(BusinessException.class, () -> service.update(3, update()));
        assertEquals(403, error.getCode());
        verify(service, never()).update(any(Wrapper.class));
        verifyNoInteractions(images);
    }

    @Test void staleVersionCannotOverwrite() {
        doReturn(product(1)).when(service).getById(10);
        ProductUpdateDTO dto = update();
        dto.setVersion(2);
        BusinessException error = assertThrows(BusinessException.class, () -> service.update(2, dto));
        assertEquals(409, error.getCode());
        verify(service, never()).update(any(Wrapper.class));
        verifyNoInteractions(images);
    }

    @ParameterizedTest
    @CsvSource({"1,0", "0,0", "2,0", "3,3"})
    void editPreservesStockAndSetsExpectedAuditState(int oldStatus, int expectedStatus) {
        doReturn(product(oldStatus)).when(service).getById(10);
        doReturn(true).when(service).update(any(Wrapper.class));
        service.update(2, update());
        LambdaUpdateWrapper<Product> wrapper = savedUpdate();
        String set = wrapper.getSqlSet();
        assertEquals("新标题", boundValue(wrapper, set, "title"));
        assertEquals(7, boundValue(wrapper, set, "stock"));
        assertEquals(expectedStatus, boundValue(wrapper, set, "status"));
        assertEquals(4, boundValue(wrapper, set, "version"));
        for (String field : List.of("audit_remark", "audit_time", "auditor_id")) {
            assertNull(boundValue(wrapper, set, field));
        }
        String where = wrapper.getSqlSegment();
        assertEquals(10, boundValue(wrapper, where, "id"));
        assertEquals(2, boundValue(wrapper, where, "user_id"));
        assertEquals(3, boundValue(wrapper, where, "version"));
        assertEquals(oldStatus, boundValue(wrapper, where, "status"));
        verifyNoInteractions(images);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.001", "100000000.00"})
    void partialEditStillChecksUnchangedPriceUsingDtoConstraints(String price) {
        Product current = product(1);
        current.setPrice(new BigDecimal(price));
        doReturn(current).when(service).getById(10);
        BusinessException error = assertThrows(BusinessException.class, () -> service.update(2, update()));
        assertEquals(400, error.getCode());
        verify(service, never()).update(any(Wrapper.class));
        verifyNoInteractions(images);
    }

    @Test void partialEditStillRejectsAnUnchangedWhitespaceTitle() {
        Product current = product(1);
        current.setTitle("   ");
        doReturn(current).when(service).getById(10);
        ProductUpdateDTO dto = update();
        dto.setTitle(null);
        BusinessException error = assertThrows(BusinessException.class, () -> service.update(2, dto));
        assertEquals(400, error.getCode());
        verify(service, never()).update(any(Wrapper.class));
    }

    @Test void clearOriginalPriceIsSavedAsNull() {
        Product current = product(1);
        current.setOriginalPrice(new BigDecimal("30"));
        doReturn(current).when(service).getById(10);
        doReturn(true).when(service).update(any(Wrapper.class));
        ProductUpdateDTO dto = update();
        dto.setClearOriginalPrice(true);
        service.update(2, dto);
        LambdaUpdateWrapper<Product> wrapper = savedUpdate();
        assertNull(boundValue(wrapper, wrapper.getSqlSet(), "original_price"));
    }

    @Test void conflictDoesNotReplaceImageRows() {
        doReturn(product(1)).when(service).getById(10);
        doReturn(false).when(service).update(any(Wrapper.class));
        ProductUpdateDTO dto = update();
        dto.setImages(List.of(image("/files/a.png", 0)));
        BusinessException error = assertThrows(BusinessException.class, () -> service.update(2, dto));
        assertEquals(409, error.getCode());
        verifyNoInteractions(images);
    }

    @Test void orderedProductCannotBeDeleted() {
        doReturn(product(1)).when(service).getById(10);
        when(orders.selectCount(any(Wrapper.class))).thenReturn(1L);
        BusinessException error = assertThrows(BusinessException.class, () -> service.deleteById(2, 10, false));
        assertEquals(409, error.getCode());
        verifyNoInteractions(images);
        verify(service, never()).removeById(anyInt());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingOrDisabledCategoryCannotBePublished(boolean disabled) {
        if (disabled) {
            Category category = new Category();
            category.setStatus(0);
            when(categories.selectById(1)).thenReturn(category);
        }
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.add(2, publish(List.of(image("/files/a.png", 0)))));
        assertEquals(400, error.getCode());
        verify(service, never()).save(any(Product.class));
        verifyNoInteractions(images);
    }

    @ParameterizedTest
    @ValueSource(strings = {"publish", "edit"})
    void duplicateImagesAreRejectedBeforeWriting(String operation) {
        assertRejectedAlbum(operation, List.of(image("/files/a.png", 0), image("/files/a.png", 0)),
                "请勿重复添加同一张图片");
    }

    @ParameterizedTest
    @ValueSource(strings = {"publish", "edit"})
    void twoCoverImagesAreRejectedBeforeWriting(String operation) {
        assertRejectedAlbum(operation, List.of(image("/files/a.png", 1), image("/files/b.png", 1)),
                "只能设置一张封面图片");
    }

    @ParameterizedTest
    @ValueSource(strings = {"publish", "edit"})
    void anotherUsersNewImageIsRejected(String operation) {
        assertRejectedAlbum(operation, List.of(image("/files/products/3/a.png", 1)),
                "不能使用其他用户上传的商品图片");
    }

    @ParameterizedTest
    @CsvSource({"publish,https://example.com/a.png", "edit,https://example.com/a.png",
            "publish,/files/../a.png", "edit,/files/../a.png", "publish,/files/a.svg", "edit,/files/a.svg",
            "publish,/files/a.png?x=1", "edit,/files/a.png?x=1"})
    void unsafeImageReferencesAreRejected(String operation, String url) {
        assertRejectedAlbum(operation, List.of(image(url, 1)), "请使用本站上传的JPG/PNG图片");
    }

    @ParameterizedTest
    @CsvSource({"publish,0", "edit,0", "publish,10", "edit,10"})
    void internalCallsAlsoRejectEmptyOrOversizedAlbums(String operation, int size) {
        assertRejectedAlbum(operation, Collections.nCopies(size, image("/files/a.png", 0)), "请保留1至9张商品图片");
    }

    @ParameterizedTest
    @ValueSource(strings = {"publish", "edit"})
    void nullImageElementsAreRejectedWithoutNullPointerException(String operation) {
        assertRejectedAlbum(operation, Collections.singletonList(null), "图片地址不能为空");
    }

    @ParameterizedTest
    @CsvSource({"publish,false", "edit,false", "publish,true", "edit,true"})
    void validAlbumsKeepOwnershipCoverAndNormalizedOrder(String operation, boolean explicitSecondCover) {
        List<ProductImageDTO> album = List.of(image("  /files/products/2/a.png  ", 0),
                image("/files/b.jpg", explicitSecondCover ? 1 : 0));
        if (operation.equals("publish")) {
            enabledCategory();
            successfulSave();
            service.add(2, publish(album));
            ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
            verify(service).save(saved.capture());
            assertEquals("教材", saved.getValue().getTitle());
            assertEquals(0, saved.getValue().getStock());
            assertEquals(0, saved.getValue().getVersion());
            assertEquals(0, saved.getValue().getStatus());
        } else {
            doReturn(product(1)).when(service).getById(10);
            doReturn(true).when(service).update(any(Wrapper.class));
            ProductUpdateDTO dto = update();
            dto.setImages(album);
            service.update(2, dto);
            verify(images).delete(any(Wrapper.class));
        }
        ArgumentCaptor<ProductImage> savedImages = ArgumentCaptor.forClass(ProductImage.class);
        verify(images, times(2)).insert(savedImages.capture());
        List<ProductImage> saved = savedImages.getAllValues();
        assertEquals(List.of("/files/products/2/a.png", "/files/b.jpg"), saved.stream().map(ProductImage::getImageUrl).toList());
        assertEquals(List.of(0, 1), saved.stream().map(ProductImage::getSort).toList());
        assertEquals(explicitSecondCover ? List.of(0, 1) : List.of(1, 0), saved.stream().map(ProductImage::getIsCover).toList());
        assertTrue(saved.stream().allMatch(image -> image.getProductId().equals(10)));
        assertEquals(99, album.getFirst().getSort(), "整理图册不能修改调用者传入的对象");
    }

    @Test void failedProductSaveDoesNotWriteImages() {
        enabledCategory();
        doReturn(false).when(service).save(any(Product.class));
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.add(2, publish(List.of(image("/files/a.png", 0)))));
        assertEquals(409, error.getCode());
        verifyNoInteractions(images);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void listAndCountNormalizeKeywordsBeforeCallingMapper(boolean count) {
        ProductQueryDTO dto = new ProductQueryDTO();
        dto.setTitle("  教材  ");
        if (count) {
            when(products.listPageCount(any())).thenReturn(3);
            assertEquals(3, service.listPageCount(dto));
            verify(products).listPageCount(argThat(query -> query.getTitle().equals("教材")));
        } else {
            when(products.listPage(any())).thenReturn(List.of());
            assertTrue(service.listPage(dto).isEmpty());
            verify(products).listPage(argThat(query -> query.getTitle().equals("教材")));
        }
    }
}
