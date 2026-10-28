package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.exception.BusinessException;
import com.example.utils.SensitiveWordUtil;
import com.example.mapper.OrderMapper;
import com.example.mapper.ProductMapper;
import com.example.mapper.ProductReviewMapper;
import com.example.mapper.ReviewImageMapper;
import com.example.pojo.dto.ProductReviewAddDTO;
import com.example.pojo.dto.ProductReviewQueryDTO;
import com.example.pojo.dto.ProductReviewReplyDTO;
import com.example.pojo.entity.Order;
import com.example.pojo.entity.Product;
import com.example.pojo.entity.ProductReview;
import com.example.pojo.entity.ReviewImage;
import com.example.pojo.vo.ProductReviewVO;
import com.example.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品评价业务逻辑接口实现类
 */
@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl extends ServiceImpl<ProductReviewMapper, ProductReview> implements ProductReviewService {

    private final ProductReviewMapper productReviewMapper;
    private final ReviewImageMapper reviewImageMapper;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;

    /** 订单状态：已完成 */
    private static final int ORDER_COMPLETED = 3;

    /** 评价状态：显示 */
    private static final int REVIEW_VISIBLE = 1;
    /** 评价状态：隐藏 */
    private static final int REVIEW_HIDDEN = 0;

    /**
     * 分页查询商品评价列表（联表 product、base_user）
     */
    @Override
    public List<ProductReviewVO> listPage(ProductReviewQueryDTO queryDTO) {
        List<ProductReviewVO> list = productReviewMapper.listPage(queryDTO);
        if (list == null || list.isEmpty()) {
            return list;
        }

        // 批量查询评价图片
        List<Integer> reviewIds = list.stream()
                .map(ProductReviewVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!reviewIds.isEmpty()) {
            List<ReviewImage> images = reviewImageMapper.selectList(
                    new LambdaQueryWrapper<ReviewImage>()
                            .in(ReviewImage::getReviewId, reviewIds)
                            .orderByAsc(ReviewImage::getSort)
            );

            Map<Integer, List<String>> imageMap = new HashMap<>();
            for (ReviewImage img : images) {
                imageMap.computeIfAbsent(img.getReviewId(), k -> new ArrayList<>())
                        .add(img.getImageUrl());
            }

            for (ProductReviewVO vo : list) {
                vo.setImages(imageMap.getOrDefault(vo.getId(), Collections.emptyList()));
            }
        }

        return list;
    }

    /**
     * 查询评价总数
     */
    @Override
    public Integer listPageCount(ProductReviewQueryDTO queryDTO) {
        return productReviewMapper.listPageCount(queryDTO);
    }

    /**
     * 买家对已完成的订单进行评价
     * <p>校验：订单必须已完成、未被评价、且为本人订单</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Integer buyerId, ProductReviewAddDTO addDTO) {
        // 校验订单
        Order order = orderMapper.selectById(addDTO.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!Objects.equals(order.getBuyerId(), buyerId)) {
            throw new BusinessException("只能评价自己的订单");
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != ORDER_COMPLETED) {
            throw new BusinessException("只有已完成的订单才能评价");
        }

        // 校验是否已评价
        Long count = this.lambdaQuery()
                .eq(ProductReview::getOrderId, addDTO.getOrderId())
                .count();
        if (count > 0) {
            throw new BusinessException("该订单已评价，不能重复评价");
        }

        // 敏感词过滤：对评价内容进行检测和替换
        String filteredContent = SensitiveWordUtil.replace(addDTO.getContent());

        // 构建评价实体
        ProductReview review = ProductReview.builder()
                .orderId(addDTO.getOrderId())
                .productId(order.getProductId())
                .buyerId(buyerId)
                .sellerId(order.getSellerId())
                .rating(addDTO.getRating())
                .content(filteredContent)
                .status(REVIEW_VISIBLE)
                .createTime(LocalDateTime.now())
                .build();
        this.save(review);

        // 保存评价图片
        if (addDTO.getImages() != null && !addDTO.getImages().isEmpty()) {
            saveReviewImages(review.getId(), addDTO.getImages());
        }

        // 更新商品的评价统计（评价数 + 平均评分）
        updateProductReviewStats(order.getProductId());
    }

    /**
     * 卖家回复评价
     * <p>校验：只有该商品的卖家才能回复</p>
     */
    @Override
    public void reply(Integer sellerId, ProductReviewReplyDTO replyDTO) {
        ProductReview review = this.getById(replyDTO.getId());
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        if (!Objects.equals(review.getSellerId(), sellerId)) {
            throw new BusinessException("只能回复自己商品的评价");
        }
        if (review.getReplyContent() != null && !review.getReplyContent().isEmpty()) {
            throw new BusinessException("该评价已回复，不能重复回复");
        }

        // 敏感词过滤：对卖家回复内容进行检测和替换
        String filteredReplyContent = SensitiveWordUtil.replace(replyDTO.getReplyContent());

        LambdaUpdateWrapper<ProductReview> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductReview::getId, replyDTO.getId())
                .set(ProductReview::getReplyContent, filteredReplyContent)
                .set(ProductReview::getReplyTime, LocalDateTime.now());
        this.update(wrapper);
    }

    /**
     * 管理员隐藏/显示评价
     */
    @Override
    public void toggleStatus(Integer id, Integer status) {
        ProductReview review = this.getById(id);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        if (status != REVIEW_VISIBLE && status != REVIEW_HIDDEN) {
            throw new BusinessException("状态值无效：0-隐藏，1-显示");
        }

        LambdaUpdateWrapper<ProductReview> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductReview::getId, id)
                .set(ProductReview::getStatus, status);
        this.update(wrapper);

        // 更新商品的评价统计
        if (review.getProductId() != null) {
            updateProductReviewStats(review.getProductId());
        }
    }

    /**
     * 查询商品评价统计（评价数 + 平均评分）
     */
    @Override
    public Map<String, Object> getReviewStats(Integer productId) {
        Map<String, Object> stats = new HashMap<>();
        Double avgRating = productReviewMapper.getAvgRatingByProductId(productId);
        Integer reviewCount = productReviewMapper.getReviewCountByProductId(productId);
        stats.put("avgRating", avgRating != null ? avgRating : 0.0);
        stats.put("reviewCount", reviewCount != null ? reviewCount : 0);
        return stats;
    }

    /**
     * 查询当前买家是否已评价指定订单
     */
    @Override
    public boolean isReviewed(Integer orderId) {
        Long count = this.lambdaQuery()
                .eq(ProductReview::getOrderId, orderId)
                .count();
        return count > 0;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 保存评价图片
     *
     * @param reviewId  评价ID
     * @param imageUrls 图片URL列表
     */
    private void saveReviewImages(Integer reviewId, List<String> imageUrls) {
        List<ReviewImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(ReviewImage.builder()
                    .reviewId(reviewId)
                    .imageUrl(imageUrls.get(i))
                    .sort(i)
                    .createTime(LocalDateTime.now())
                    .build());
        }
        images.forEach(reviewImageMapper::insert);
    }

    /**
     * 更新商品表的评价统计冗余字段
     *
     * @param productId 商品ID
     */
    private void updateProductReviewStats(Integer productId) {
        Double avgRating = productReviewMapper.getAvgRatingByProductId(productId);
        Integer reviewCount = productReviewMapper.getReviewCountByProductId(productId);

        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Product::getId, productId)
                .set(Product::getReviewCount, reviewCount != null ? reviewCount : 0)
                .set(Product::getRating, avgRating != null ? avgRating : 0.0);
        productMapper.update(null, wrapper);
    }
}
