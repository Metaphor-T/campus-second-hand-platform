package com.example.product;

import com.example.exception.BusinessException;
import com.example.pojo.dto.*;
import com.example.pojo.entity.Product;
import com.example.utils.ProductRules;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProductRulesTest {
    static ValidatorFactory factory;
    static Validator validator;
    @BeforeAll static void init() { factory = Validation.buildDefaultValidatorFactory(); validator = factory.getValidator(); }
    @AfterAll static void close() { factory.close(); }
    static Product valid() { return Product.builder().categoryId(1).title("高等数学教材").price(new BigDecimal("15.50")).condition(2).tradeType(1).stock(1).build(); }
    static ProductImageDTO image(String path, int cover) { return ProductImageDTO.builder().imageUrl(path).isCover(cover).build(); }
    @ParameterizedTest @ValueSource(strings={"0", "-1", "0.001", "100000000.00"})
    void invalidSellingPrice(String price) { Product p = valid(); p.setPrice(new BigDecimal(price)); assertThrows(BusinessException.class, () -> ProductRules.validate(p)); }
    @Test void whitespaceTitleIsRejected() { Product p = valid(); p.setTitle("   "); assertThrows(BusinessException.class, () -> ProductRules.validate(p)); }
    @Test void validProductAndUnlimitedStock() { Product p = valid(); p.setStock(0); assertDoesNotThrow(() -> ProductRules.validate(p)); }
    @Test void missingCoverDefaultsToFirstImage() { var result = ProductRules.images(List.of(image("/files/a.png",0),image("/files/b.jpg",0)),2); assertEquals(1,result.get(0).getIsCover()); assertEquals(0,result.get(1).getIsCover()); assertEquals(1,result.get(1).getSort()); }
    @Test void duplicateImagesAreRejected() { assertThrows(BusinessException.class, () -> ProductRules.images(List.of(image("/files/a.png",0),image("/files/a.png",0)),2)); }
    @Test void twoCoversAreRejected() { assertThrows(BusinessException.class, () -> ProductRules.images(List.of(image("/files/a.png",1),image("/files/b.png",1)),2)); }
    @Test void anotherUsersNewImageIsRejected() { assertThrows(BusinessException.class, () -> ProductRules.images(List.of(image("/files/products/3/a.png",1)),2)); }
    @ParameterizedTest @ValueSource(strings={"https://example.com/a.png", "/files/../a.png", "/files/a.svg", "/files/a.png?x=1"})
    void unsafeImageReferencesAreRejected(String url) { assertThrows(BusinessException.class, () -> ProductRules.images(List.of(image(url,1)),2)); }
    @Test void imagesCannotBeEmptyOrExceedNine() { assertThrows(BusinessException.class, () -> ProductRules.images(List.of(),2)); assertThrows(BusinessException.class, () -> ProductRules.images(Collections.nCopies(10,image("/files/a.png",0)),2)); }
    @Test void invalidQueryRangesAndSortAreRejected() { ProductQueryDTO q=new ProductQueryDTO(); q.setMinPrice(BigDecimal.TEN); q.setMaxPrice(BigDecimal.ONE); assertFalse(validator.validate(q).isEmpty()); q.setMinPrice(null); q.setSortBy("price; DROP TABLE product"); assertFalse(validator.validate(q).isEmpty()); }
    @Test void invalidPageCannotReachPagerAspect() { ProductQueryDTO q=new ProductQueryDTO(); q.setCurrent(0); assertFalse(validator.validate(q).isEmpty()); q.setCurrent(1); q.setSize(51); assertFalse(validator.validate(q).isEmpty()); }
    @Test void nestedImageValidationRuns() { ProductAddDTO dto=new ProductAddDTO(); dto.setCategoryId(1); dto.setTitle("教材"); dto.setPrice(BigDecimal.ONE); dto.setCondition(1); dto.setTradeType(1); dto.setImages(List.of(image("",1))); assertFalse(validator.validate(dto).isEmpty()); }
}
