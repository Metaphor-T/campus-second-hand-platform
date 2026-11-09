package com.example.product;

import com.example.mapper.ItemSimilarityMapper;
import com.example.mapper.ProductImageMapper;
import com.example.mapper.RecommendMapper;
import com.example.pojo.entity.ItemSimilarity;
import com.example.pojo.entity.Product;
import com.example.pojo.vo.ProductVO;
import com.example.pojo.vo.UserBehaviorVO;
import com.example.service.ProductService;
import com.example.service.impl.RecommendServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecommendServiceTest {
    private RecommendMapper behaviors;
    private ItemSimilarityMapper similarities;
    private ProductService products;
    private RecommendServiceImpl service;

    @BeforeEach
    void setup() {
        behaviors = mock(RecommendMapper.class);
        similarities = mock(ItemSimilarityMapper.class);
        products = mock(ProductService.class);
        ProductImageMapper images = mock(ProductImageMapper.class);
        when(images.selectList(any())).thenReturn(List.of());
        service = new RecommendServiceImpl(behaviors, similarities, products, images);
    }

    private UserBehaviorVO behavior(int userId, int productId, int type) {
        return UserBehaviorVO.builder().userId(userId).productId(productId)
                .behaviorType(type).score(1).build();
    }

    private ItemSimilarity similarity(int first, int second, String score) {
        return ItemSimilarity.builder().productId1(first).productId2(second)
                .similarity(new BigDecimal(score)).build();
    }

    private void hotProducts() {
        when(products.listHotProducts()).thenReturn(List.of(
                ProductVO.builder().id(90).status(1).build()));
    }

    private void candidates() {
        when(behaviors.selectUserBehaviors(2)).thenReturn(List.of(behavior(2, 10, 3)));
        when(similarities.selectList(any())).thenReturn(List.of(
                similarity(10, 20, "0.8"), similarity(10, 30, "0.5")));
    }

    @Test
    void anonymousUserReceivesHotProducts() {
        hotProducts();
        assertEquals(90, service.recommendForUser(null, 10).getFirst().getId());
        verifyNoInteractions(behaviors, similarities);
    }

    @Test
    void userWithoutBehaviorReceivesHotProducts() {
        when(behaviors.selectUserBehaviors(2)).thenReturn(List.of());
        hotProducts();
        assertEquals(90, service.recommendForUser(2, 10).getFirst().getId());
        verifyNoInteractions(similarities);
    }

    @Test
    void emptyCandidatePoolReceivesHotProducts() {
        when(behaviors.selectUserBehaviors(2)).thenReturn(List.of(behavior(2, 10, 3)));
        when(similarities.selectList(any())).thenReturn(List.of());
        hotProducts();
        assertEquals(90, service.recommendForUser(2, 10).getFirst().getId());
    }

    @Test
    void allUnavailableCandidatesFallBackToHotProducts() {
        candidates();
        when(products.listByIds(anyCollection())).thenReturn(List.of(
                Product.builder().id(20).status(3).build(),
                Product.builder().id(30).status(0).build()));
        hotProducts();
        assertEquals(List.of(90), service.recommendForUser(2, 10).stream()
                .map(ProductVO::getId).toList());
        verify(products).listHotProducts();
    }

    @Test
    void deletedCandidatesFallBackToHotProducts() {
        candidates();
        when(products.listByIds(anyCollection())).thenReturn(List.of());
        hotProducts();
        assertEquals(90, service.recommendForUser(2, 10).getFirst().getId());
    }

    @Test
    void availableRecommendationsKeepTheirOrderAndSkipHotFallback() {
        candidates();
        // Database return order must not replace the predicted-score order.
        when(products.listByIds(anyCollection())).thenReturn(List.of(
                Product.builder().id(30).status(1).build(),
                Product.builder().id(20).status(1).build()));
        assertEquals(List.of(20, 30), service.recommendForUser(2, 10).stream()
                .map(ProductVO::getId).toList());
        verify(products, never()).listHotProducts();
    }

    @Test
    void partiallyAvailableCandidatesKeepTheAvailableProducts() {
        candidates();
        when(products.listByIds(anyCollection())).thenReturn(List.of(
                Product.builder().id(20).status(3).build(),
                Product.builder().id(30).status(1).build()));
        assertEquals(List.of(30), service.recommendForUser(2, 2).stream()
                .map(ProductVO::getId).toList());
        verify(products, never()).listHotProducts();
    }

    @Test
    void recomputeStoresCosineSimilarityWithNormalizedProductPair() {
        when(behaviors.selectAllBehaviors()).thenReturn(List.of(
                behavior(2, 10, 3), behavior(2, 20, 2),
                behavior(3, 10, 3), behavior(3, 20, 2)));
        service.recomputeSimilarities();
        ArgumentCaptor<ItemSimilarity> captured = ArgumentCaptor.forClass(ItemSimilarity.class);
        verify(similarities).insert(captured.capture());
        assertEquals(10, captured.getValue().getProductId1());
        assertEquals(20, captured.getValue().getProductId2());
        assertEquals(0, BigDecimal.ONE.compareTo(captured.getValue().getSimilarity()));
    }
}
