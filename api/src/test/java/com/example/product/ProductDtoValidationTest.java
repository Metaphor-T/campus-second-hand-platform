package com.example.product;

import com.example.pojo.dto.*;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductDtoValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll static void setupValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll static void closeValidator() { factory.close(); }

    private ProductImageDTO image() {
        return ProductImageDTO.builder().imageUrl("/files/book.png").isCover(1).sort(0).build();
    }

    private ProductAddDTO validAdd() {
        ProductAddDTO dto = new ProductAddDTO();
        dto.setCategoryId(1);
        dto.setTitle("高等数学教材");
        dto.setPrice(new BigDecimal("15.50"));
        dto.setCondition(2);
        dto.setTradeType(1);
        dto.setStock(1);dto.setStockMode(0);
        dto.setImages(List.of(image()));
        return dto;
    }

    private void assertInvalid(Object dto, String property) {
        assertTrue(validator.validate(dto).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(property)),
                () -> "应拒绝字段 " + property);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.001", "100000000.00"})
    void invalidSellingPriceIsRejected(String value) {
        ProductAddDTO dto = validAdd();
        dto.setPrice(new BigDecimal(value));
        assertInvalid(dto, "price");
        ProductUpdateDTO edit = new ProductUpdateDTO();
        edit.setId(10);
        edit.setPrice(new BigDecimal(value));
        assertInvalid(edit, "price");
    }

    @Test void missingSellingPriceIsRejected() {
        ProductAddDTO dto = validAdd();
        dto.setPrice(null);
        assertInvalid(dto, "price");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void missingOrWhitespaceTitleIsRejected(String title) {
        ProductAddDTO dto = validAdd();
        dto.setTitle(title);
        assertInvalid(dto, "title");
    }

    @Test void titleAndDescriptionLengthAreLimited() {
        ProductAddDTO dto = validAdd();
        dto.setTitle("书".repeat(201));
        assertInvalid(dto, "title");
        dto = validAdd();
        dto.setDescription("字".repeat(2001));
        assertInvalid(dto, "description");
    }

    @Test void validProductAllowsUnlimitedStockAndOptionalOriginalPrice() {
        ProductAddDTO dto = validAdd();
        dto.setStock(0);
        assertTrue(validator.validate(dto).isEmpty());
        dto.setOriginalPrice(BigDecimal.ZERO);
        assertTrue(validator.validate(dto).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0.001", "100000000.00"})
    void invalidOriginalPriceIsRejected(String value) {
        ProductAddDTO dto = validAdd();
        dto.setOriginalPrice(new BigDecimal(value));
        assertInvalid(dto, "originalPrice");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1000000})
    void invalidStockIsRejected(int stock) {
        ProductAddDTO dto = validAdd();
        dto.setStock(stock);
        assertInvalid(dto, "stock");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void invalidCategoryIdIsRejected(int id) {
        ProductAddDTO dto = validAdd();
        dto.setCategoryId(id);
        assertInvalid(dto, "categoryId");
    }

    @Test void conditionAndTradeTypeRangesAreChecked() {
        ProductAddDTO dto = validAdd();
        dto.setCondition(5);
        dto.setTradeType(0);
        assertInvalid(dto, "condition");
        assertInvalid(dto, "tradeType");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10})
    void imagesCannotBeEmptyOrExceedNine(int size) {
        ProductAddDTO dto = validAdd();
        dto.setImages(Collections.nCopies(size, image()));
        assertInvalid(dto, "images");
    }

    @Test void imagesAreRequiredForPublishing() {
        ProductAddDTO dto = validAdd();
        dto.setImages(null);
        assertInvalid(dto, "images");
    }

    @Test void nestedImageConstraintsAreChecked() {
        ProductAddDTO dto = validAdd();
        ProductImageDTO image = dto.getImages().getFirst();
        image.setImageUrl("");
        assertInvalid(dto, "images[0].imageUrl");
        image.setImageUrl("a".repeat(256));
        assertInvalid(dto, "images[0].imageUrl");
        image.setImageUrl("/files/book.png");
        image.setIsCover(2);
        image.setSort(-1);
        assertInvalid(dto, "images[0].isCover");
        assertInvalid(dto, "images[0].sort");
    }

    @Test void partialEditAllowsOmittedFieldsButRequiresProductId() {
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setId(10);
        assertTrue(validator.validate(dto).isEmpty());
        dto.setId(null);
        assertInvalid(dto, "id");
        dto.setId(0);
        assertInvalid(dto, "id");
    }

    @Test void editRejectsWhitespaceTitleAndNegativeVersion() {
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setId(10);
        dto.setTitle("   ");
        dto.setVersion(-1);
        assertInvalid(dto, "title");
        assertInvalid(dto, "version");
    }

    @Test void editAlsoValidatesNestedImages() {
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setId(10);
        dto.setImages(List.of(ProductImageDTO.builder().imageUrl("").build()));
        assertInvalid(dto, "images[0].imageUrl");
    }

    @Test void reversedPricesAndUnknownSortAreRejected() {
        ProductQueryDTO dto = new ProductQueryDTO();
        dto.setMinPrice(BigDecimal.TEN);
        dto.setMaxPrice(BigDecimal.ONE);
        assertInvalid(dto, "priceRangeValid");
        dto.setMinPrice(null);
        dto.setSortBy("price; DROP TABLE product");
        assertInvalid(dto, "sortBy");
    }

    @ParameterizedTest
    @CsvSource({"0,12,current", "100001,12,current", "1,0,size", "1,101,size"})
    void invalidPaginationIsRejectedBeforeOffsetConversion(int page, int size, String property) {
        ProductQueryDTO dto = new ProductQueryDTO();
        dto.setCurrent(page);
        dto.setSize(size);
        assertInvalid(dto, property);
    }

    @Test void administratorPageSize100IsSupported() {
        ProductQueryDTO dto = new ProductQueryDTO();
        dto.setSize(100);
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test void queryFilterRangesAreChecked() {
        ProductQueryDTO dto = new ProductQueryDTO();
        dto.setCategoryId(0);
        dto.setUserId(-1);
        dto.setStatus(4);
        dto.setCondition(5);
        dto.setTradeType(0);
        for (String property : List.of("categoryId", "userId", "status", "condition", "tradeType")) {
            assertInvalid(dto, property);
        }
    }
}
