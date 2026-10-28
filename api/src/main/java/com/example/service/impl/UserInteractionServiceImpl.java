package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mapper.ProductFavoriteMapper;
import com.example.mapper.ProductImageMapper;
import com.example.mapper.ProductMapper;
import com.example.mapper.ProductViewMapper;
import com.example.mapper.ProductWantMapper;
import com.example.pojo.entity.Product;
import com.example.pojo.entity.ProductFavorite;
import com.example.pojo.entity.ProductImage;
import com.example.pojo.entity.ProductView;
import com.example.pojo.entity.ProductWant;
import com.example.pojo.vo.ProductImageVO;
import com.example.pojo.vo.ProductVO;
import com.example.service.ProductService;
import com.example.service.UserInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户交互业务逻辑接口实现类
 * <p>处理用户对商品的收藏、想要等交互行为</p>
 */
@Service
@RequiredArgsConstructor
public class UserInteractionServiceImpl implements UserInteractionService {

    private final ProductFavoriteMapper productFavoriteMapper;
    private final ProductWantMapper productWantMapper;
    private final ProductViewMapper productViewMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductService productService;

    /**
     * 切换收藏状态（已收藏则取消，未收藏则添加）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Integer userId, Integer productId) {
        // 检查是否已收藏
        ProductFavorite existing = productFavoriteMapper.selectOne(
                new LambdaQueryWrapper<ProductFavorite>()
                        .eq(ProductFavorite::getUserId, userId)
                        .eq(ProductFavorite::getProductId, productId)
        );

        if (existing != null) {
            // 已收藏 → 取消收藏
            productFavoriteMapper.deleteById(existing.getId());
            // 更新商品收藏数（减1，不低于0）
            Product product = productService.getById(productId);
            if (product != null && product.getFavoriteCount() != null && product.getFavoriteCount() > 0) {
                productService.lambdaUpdate()
                        .eq(Product::getId, productId)
                        .set(Product::getFavoriteCount, product.getFavoriteCount() - 1)
                        .update();
            }
            return false;
        } else {
            // 未收藏 → 添加收藏
            ProductFavorite favorite = ProductFavorite.builder()
                    .userId(userId)
                    .productId(productId)
                    .createTime(LocalDateTime.now())
                    .build();
            productFavoriteMapper.insert(favorite);
            // 更新商品收藏数（加1）
            Product product = productService.getById(productId);
            if (product != null) {
                int count = (product.getFavoriteCount() != null ? product.getFavoriteCount() : 0) + 1;
                productService.lambdaUpdate()
                        .eq(Product::getId, productId)
                        .set(Product::getFavoriteCount, count)
                        .update();
            }
            return true;
        }
    }

    /**
     * 添加"想要"记录（每个用户只能添加一次）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addWant(Integer userId, Integer productId) {
        // 检查是否已想要
        Long count = productWantMapper.selectCount(
                new LambdaQueryWrapper<ProductWant>()
                        .eq(ProductWant::getUserId, userId)
                        .eq(ProductWant::getProductId, productId)
        );

        if (count > 0) {
            return; // 已存在，忽略重复操作
        }

        // 添加想要记录
        ProductWant want = ProductWant.builder()
                .userId(userId)
                .productId(productId)
                .createTime(LocalDateTime.now())
                .build();
        productWantMapper.insert(want);

        // 更新商品想要数（加1）
        Product product = productService.getById(productId);
        if (product != null) {
            int wantCount = (product.getWantCount() != null ? product.getWantCount() : 0) + 1;
            productService.lambdaUpdate()
                    .eq(Product::getId, productId)
                    .set(Product::getWantCount, wantCount)
                    .update();
        }
    }

    /**
     * 查询用户是否已收藏指定商品
     */
    @Override
    public boolean isFavorited(Integer userId, Integer productId) {
        Long count = productFavoriteMapper.selectCount(
                new LambdaQueryWrapper<ProductFavorite>()
                        .eq(ProductFavorite::getUserId, userId)
                        .eq(ProductFavorite::getProductId, productId)
        );
        return count > 0;
    }

    /**
     * 查询用户是否已"想要"指定商品
     */
    @Override
    public boolean isWanted(Integer userId, Integer productId) {
        Long count = productWantMapper.selectCount(
                new LambdaQueryWrapper<ProductWant>()
                        .eq(ProductWant::getUserId, userId)
                        .eq(ProductWant::getProductId, productId)
        );
        return count > 0;
    }

    /**
     * 查询用户收藏的商品列表（分页）
     * <p>按收藏时间倒序，附带封面图</p>
     */
    @Override
    public List<ProductVO> listFavoriteProducts(Integer userId, Integer current, Integer size) {
        // 先查出用户收藏的商品ID列表（按收藏时间倒序）
        List<ProductFavorite> favorites = productFavoriteMapper.selectList(
                new LambdaQueryWrapper<ProductFavorite>()
                        .eq(ProductFavorite::getUserId, userId)
                        .orderByDesc(ProductFavorite::getCreateTime)
        );

        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> allProductIds = favorites.stream()
                .map(ProductFavorite::getProductId)
                .collect(Collectors.toList());

        // 手动分页
        int fromIndex = current != null ? current : 0;
        int pageSize = size != null ? size : 10;
        int toIndex = Math.min(fromIndex + pageSize, allProductIds.size());
        List<Integer> pageIds = allProductIds.subList(fromIndex, toIndex);

        if (pageIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询商品
        List<Product> products = productService.listByIds(pageIds);

        // 批量查询图册（仅封面）
        List<ProductImage> allImages = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .in(ProductImage::getProductId, pageIds)
                        .orderByAsc(ProductImage::getSort)
        );
        Map<Integer, List<ProductImageVO>> imageMap = new HashMap<>();
        for (ProductImage img : allImages) {
            imageMap.computeIfAbsent(img.getProductId(), k -> new ArrayList<>())
                    .add(ProductImageVO.builder()
                            .id(img.getId())
                            .productId(img.getProductId())
                            .imageUrl(img.getImageUrl())
                            .isCover(img.getIsCover())
                            .sort(img.getSort())
                            .build());
        }

        // 构建 VO 并按收藏顺序排序
        Map<Integer, ProductVO> voMap = new LinkedHashMap<>();
        for (Product product : products) {
            ProductVO vo = ProductVO.builder()
                    .id(product.getId())
                    .userId(product.getUserId())
                    .categoryId(product.getCategoryId())
                    .title(product.getTitle())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .originalPrice(product.getOriginalPrice())
                    .condition(product.getCondition())
                    .tradeType(product.getTradeType())
                    .status(product.getStatus())
                    .viewCount(product.getViewCount())
                    .wantCount(product.getWantCount())
                    .favoriteCount(product.getFavoriteCount())
                    .createTime(product.getCreateTime())
                    .updateTime(product.getUpdateTime())
                    .images(imageMap.getOrDefault(product.getId(), Collections.emptyList()))
                    .build();
            voMap.put(product.getId(), vo);
        }

        List<ProductVO> result = new ArrayList<>();
        for (Integer pid : pageIds) {
            ProductVO vo = voMap.get(pid);
            if (vo != null) {
                result.add(vo);
            }
        }
        return result;
    }

    /**
     * 查询用户收藏商品总数
     */
    @Override
    public Integer listFavoriteCount(Integer userId) {
        Long count = productFavoriteMapper.selectCount(
                new LambdaQueryWrapper<ProductFavorite>()
                        .eq(ProductFavorite::getUserId, userId)
        );
        return count.intValue();
    }

    /**
     * 记录商品浏览
     * <p>插入一条浏览记录到 product_view 表，同时更新 product 表的 view_count 冗余字段</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordView(Integer userId, Integer productId) {
        // 插入浏览记录
        ProductView view = ProductView.builder()
                .userId(userId)
                .productId(productId)
                .createTime(LocalDateTime.now())
                .build();
        productViewMapper.insert(view);

        // 更新商品浏览数（加1）
        Product product = productService.getById(productId);
        if (product != null) {
            int viewCount = (product.getViewCount() != null ? product.getViewCount() : 0) + 1;
            productService.lambdaUpdate()
                    .eq(Product::getId, productId)
                    .set(Product::getViewCount, viewCount)
                    .update();
        }
    }
}
