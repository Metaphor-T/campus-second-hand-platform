package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mapper.ItemSimilarityMapper;
import com.example.mapper.ProductImageMapper;
import com.example.mapper.RecommendMapper;
import com.example.pojo.entity.ItemSimilarity;
import com.example.pojo.entity.ProductImage;
import com.example.pojo.vo.ProductImageVO;
import com.example.pojo.vo.ProductVO;
import com.example.pojo.vo.UserBehaviorVO;
import com.example.service.ProductService;
import com.example.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐系统业务逻辑实现类 - 基于物品的协同过滤（Item-Based Collaborative Filtering）
 * <p>
 * 核心算法思路：
 * <pre>
 * 1. 行为加权评分：将用户的收藏(5)、想要(3)、评价(4×评分)、下单(10) 四个维度的行为
 *    加权合并为用户-商品综合评分矩阵 R(u,i)
 *
 * 2. 物品相似度计算（余弦相似度）：
 *    sim(i,j) = Σu R(u,i)×R(u,j) / (√Σu R(u,i)² × √Σu R(u,j)²)
 *    仅计算存在共同用户的商品对，定期预存到 item_similarity 表
 *
 * 3. 推荐生成：对目标用户 u，取其交互过的商品集合 H，
 *    pred(u,j) = Σ(i∈H) sim(i,j) × R(u,i)
 *    取 Top N 且排除已交互商品
 *
 * 4. 冷启动：无历史行为的用户直接返回热门商品
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    // ==================== 各行为权重配置 ====================
    /** 浏览权重（当前未用于 CF，仅用于热门统计） */
    private static final double WEIGHT_VIEW = 1.0;
    /** 想要权重（用户表达兴趣，中等信号） */
    private static final double WEIGHT_WANT = 3.0;
    /** 收藏权重（强烈购买意向，高信号） */
    private static final double WEIGHT_FAVORITE = 5.0;
    /** 评价行为基础权重（最终得分 = 此权重 × 评分值） */
    private static final double WEIGHT_REVIEW = 4.0;
    /** 下单权重（最强信号：实际转化） */
    private static final double WEIGHT_PURCHASE = 10.0;

    // ==================== 推荐参数 ====================
    /** 默认推荐数量 */
    private static final int DEFAULT_SIZE = 10;
    /** 每个交互商品最多参考的相似商品数 */
    private static final int MAX_SIMILAR_PER_ITEM = 10;
    /** 相似度下限（低于此值认为不相似） */
    private static final double SIMILARITY_THRESHOLD = 0.05;
    /** 定时任务：每小时重算一次相似度矩阵 */
    private static final long RECOMPUTE_INTERVAL_MS = 3600000L;

    private final RecommendMapper recommendMapper;
    private final ItemSimilarityMapper itemSimilarityMapper;
    private final ProductService productService;
    private final ProductImageMapper productImageMapper;

    // ==================== 核心推荐方法 ====================

    @Override
    public List<ProductVO> recommendForUser(Integer userId, int size) {
        log.info("===== 推荐请求: userId={}, size={} =====", userId, size);
        if (size <= 0) size = DEFAULT_SIZE;

        // 冷启动：无用户ID或无历史行为的用户，返回热门商品
        if (userId == null) {
            log.info("推荐冷启动（未登录），返回热门商品 top{}", size);
            return fillImages(productService.listHotProducts());
        }

        // 1. 查询用户行为记录
        List<UserBehaviorVO> behaviors = recommendMapper.selectUserBehaviors(userId);
        log.info("用户{}行为记录数={}", userId, behaviors != null ? behaviors.size() : 0);
        if (behaviors == null || behaviors.isEmpty()) {
            log.info("推荐冷启动（用户{}无行为），返回热门商品", userId);
            return fillImages(productService.listHotProducts());
        }

        // 2. 构建用户-商品评分向量（同一商品多种行为合并）
        Map<Integer, Double> userItemScores = buildUserItemScores(behaviors);
        Set<Integer> interactedProductIds = userItemScores.keySet();
        log.info("用户{}交互商品ID列表: {}, 评分: {}", userId, interactedProductIds, userItemScores);

        // 3. 遍历交互商品，从相似度表获取候选
        //    candidateScores: 商品ID → 加权预测得分
        Map<Integer, Double> candidateScores = new HashMap<>();

        for (Integer productId : interactedProductIds) {
            double userScore = userItemScores.get(productId);

            // 查询与当前商品最相似的 N 个商品
            List<ItemSimilarity> similarItems = querySimilarItems(productId);
            log.info("商品{}的相似商品数={}", productId, similarItems.size());

            for (ItemSimilarity sim : similarItems) {
                log.info("  相似商品对: pid1={}, pid2={}, sim={}", sim.getProductId1(), sim.getProductId2(), sim.getSimilarity());

                // 确定相似商品ID（productId_1 和 productId_2 中另一个）
                int similarProductId = sim.getProductId1().equals(productId)
                        ? sim.getProductId2() : sim.getProductId1();

                // 跳过用户已交互的商品
                if (interactedProductIds.contains(similarProductId)) {
                    log.info("  跳过已交互商品: pid={}", similarProductId);
                    continue;
                }

                double similarity = sim.getSimilarity().doubleValue();
                if (similarity < SIMILARITY_THRESHOLD) {
                    log.info("  低于相似度阈值: {}", similarity);
                    continue;
                }

                // 预测得分 = 用户对当前物品的评分 × 物品间相似度
                double increment = similarity * userScore;
                candidateScores.merge(similarProductId, increment, Double::sum);
                log.info("  候选商品{} 得分增加 {} (相似度{} × 用户评分{})", similarProductId, increment, similarity, userScore);
            }
        }

        // 4. 候选为空 → 降级为热门商品
        if (candidateScores.isEmpty()) {
            log.info("推荐候选为空（用户{}），降级返回热门商品", userId);
            return fillImages(productService.listHotProducts());
        }
        log.info("最终候选池: {}", candidateScores);

        // 5. 按预测得分降序排序，取前 N 条
        List<Integer> recommendIds = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(size)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.info("为用户{}推荐了{}个商品，候选池{}个", userId, recommendIds.size(), candidateScores.size());
        return fillImages(buildProductVOs(recommendIds));
    }

    @Override
    public List<ProductVO> refreshRecommend(Integer userId, int size) {
        // 刷新与 recommendForUser 逻辑相同，但会忽略缓存
        return recommendForUser(userId, size);
    }

    // ==================== 定时任务：重算相似度矩阵 ====================

    /**
     * 每小时自动重算全量商品相似度矩阵
     * <p>建议在系统负载较低时运行，如凌晨</p>
     */
    @Override
    @Scheduled(fixedDelay = RECOMPUTE_INTERVAL_MS)
    @Transactional(rollbackFor = Exception.class)
    public void recomputeSimilarities() {
        long start = System.currentTimeMillis();
        log.info("=== 开始重算商品相似度矩阵 ===");

        // 1. 获取全量行为数据
        List<UserBehaviorVO> allBehaviors = recommendMapper.selectAllBehaviors();
        if (allBehaviors == null || allBehaviors.isEmpty()) {
            log.warn("无行为数据，跳过相似度计算");
            return;
        }
        log.info("读取到 {} 条行为记录", allBehaviors.size());

        // 2. 构建用户-物品评分矩阵: Map<userId, Map<productId, score>>
        Map<Integer, Map<Integer, Double>> userItemMatrix = new HashMap<>();
        for (UserBehaviorVO b : allBehaviors) {
            double weight = getBehaviorWeight(b);
            userItemMatrix
                    .computeIfAbsent(b.getUserId(), k -> new HashMap<>())
                    .merge(b.getProductId(), weight, Double::sum);
        }
        log.info("构建了 {} 个用户的评分向量", userItemMatrix.size());

        // 3. 物品维度整理: Map<productId, Map<userId, score>>
        Map<Integer, Map<Integer, Double>> itemUserMatrix = transposeMatrix(userItemMatrix);
        List<Integer> allProductIds = new ArrayList<>(itemUserMatrix.keySet());
        log.info("共有 {} 个商品参与相似度计算", allProductIds.size());

        // 4. 预计算每个商品的向量模长（用于余弦相似度分母）
        Map<Integer, Double> itemNorms = new HashMap<>(itemUserMatrix.size());
        for (Map.Entry<Integer, Map<Integer, Double>> entry : itemUserMatrix.entrySet()) {
            double sumSq = 0;
            for (double score : entry.getValue().values()) {
                sumSq += score * score;
            }
            itemNorms.put(entry.getKey(), Math.sqrt(sumSq));
        }

        // 5. 逐对计算余弦相似度
        List<ItemSimilarity> similarityList = new ArrayList<>();
        int totalPairs = 0;
        int n = allProductIds.size();

        for (int i = 0; i < n; i++) {
            Integer pid1 = allProductIds.get(i);
            Map<Integer, Double> users1 = itemUserMatrix.get(pid1);
            double norm1 = itemNorms.get(pid1);

            if (norm1 == 0) continue;

            for (int j = i + 1; j < n; j++) {
                Integer pid2 = allProductIds.get(j);
                Map<Integer, Double> users2 = itemUserMatrix.get(pid2);
                double norm2 = itemNorms.get(pid2);

                if (norm2 == 0) continue;

                // 计算共同用户的点积（仅遍历较小的那个集合）
                Map<Integer, Double> smaller = users1.size() <= users2.size() ? users1 : users2;
                Map<Integer, Double> larger  = users1.size() <= users2.size() ? users2 : users1;

                double dotProduct = 0;
                for (Map.Entry<Integer, Double> entry : smaller.entrySet()) {
                    Double otherScore = larger.get(entry.getKey());
                    if (otherScore != null) {
                        dotProduct += entry.getValue() * otherScore;
                    }
                }

                if (dotProduct == 0) continue;

                double similarity = dotProduct / (norm1 * norm2);
                similarity = Math.min(1.0, Math.max(0, similarity)); // 截断到 [0, 1]

                if (similarity >= SIMILARITY_THRESHOLD) {
                    // 确保 productId_1 < productId_2
                    int p1 = Math.min(pid1, pid2);
                    int p2 = Math.max(pid1, pid2);
                    similarityList.add(ItemSimilarity.builder()
                            .productId1(p1)
                            .productId2(p2)
                            .similarity(BigDecimal.valueOf(similarity)
                                    .setScale(10, RoundingMode.HALF_UP))
                            .createTime(LocalDateTime.now())
                            .build());
                }
                totalPairs++;
            }
        }
        log.info("计算了 {} 个商品对，其中 {} 个超过相似度阈值", totalPairs, similarityList.size());

        // 6. 全量刷新相似度表
        itemSimilarityMapper.delete(null);
        if (!similarityList.isEmpty()) {
            // 批量插入，每次 500 条
            int batchSize = 500;
            for (int k = 0; k < similarityList.size(); k += batchSize) {
                int end = Math.min(k + batchSize, similarityList.size());
                List<ItemSimilarity> batch = similarityList.subList(k, end);
                // MyBatis Plus 的 saveBatch 基于 IService，但这里用 Mapper 直接批量 insert
                for (ItemSimilarity s : batch) {
                    itemSimilarityMapper.insert(s);
                }
            }
        }

        long cost = System.currentTimeMillis() - start;
        log.info("=== 相似度矩阵重算完成，耗时 {}ms，写入 {} 条记录 ===", cost, similarityList.size());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从行为记录构建用户-商品评分映射（同一商品多种行为分值合并）
     */
    private Map<Integer, Double> buildUserItemScores(List<UserBehaviorVO> behaviors) {
        Map<Integer, Double> scoreMap = new HashMap<>();
        for (UserBehaviorVO b : behaviors) {
            double weight = getBehaviorWeight(b);
            scoreMap.merge(b.getProductId(), weight, Double::sum);
        }
        return scoreMap;
    }

    /**
     * 根据行为类型获取权重值
     */
    private double getBehaviorWeight(UserBehaviorVO b) {
        int type = b.getBehaviorType();
        int score = b.getScore() != null ? b.getScore() : 1;
        return switch (type) {
            case 2 -> WEIGHT_WANT;               // 想要
            case 3 -> WEIGHT_FAVORITE;           // 收藏
            case 4 -> WEIGHT_REVIEW * score;     // 评价（权重 × 评分）
            case 5 -> WEIGHT_PURCHASE;           // 下单
            default -> 1.0;                      // 其他（含浏览）
        };
    }

    /**
     * 查询与某商品最相似的 N 个商品
     */
    private List<ItemSimilarity> querySimilarItems(Integer productId) {
        LambdaQueryWrapper<ItemSimilarity> wrapper = new LambdaQueryWrapper<ItemSimilarity>()
                .and(w -> w.eq(ItemSimilarity::getProductId1, productId)
                        .or().eq(ItemSimilarity::getProductId2, productId))
                .orderByDesc(ItemSimilarity::getSimilarity)
                .last("LIMIT " + MAX_SIMILAR_PER_ITEM);
        return itemSimilarityMapper.selectList(wrapper);
    }

    /**
     * 将 用户→商品 矩阵转置为 商品→用户
     */
    private Map<Integer, Map<Integer, Double>> transposeMatrix(
            Map<Integer, Map<Integer, Double>> userItemMatrix) {
        Map<Integer, Map<Integer, Double>> itemUserMatrix = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Double>> userEntry : userItemMatrix.entrySet()) {
            Integer userId = userEntry.getKey();
            for (Map.Entry<Integer, Double> itemEntry : userEntry.getValue().entrySet()) {
                Integer productId = itemEntry.getKey();
                Double score = itemEntry.getValue();
                itemUserMatrix
                        .computeIfAbsent(productId, k -> new HashMap<>())
                        .put(userId, score);
            }
        }
        return itemUserMatrix;
    }

    /**
     * 根据商品ID列表构建 ProductVO，并批量填充封面图
     */
    private List<ProductVO> buildProductVOs(List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 使用 MyBatis Plus 的 listByIds 批量查询
        List<com.example.pojo.entity.Product> products = productService.listByIds(productIds);
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        // 保持传入 ID 的顺序
        Map<Integer, com.example.pojo.entity.Product> productMap = new LinkedHashMap<>();
        for (com.example.pojo.entity.Product p : products) {
            productMap.put(p.getId(), p);
        }

        List<ProductVO> result = new ArrayList<>();
        for (Integer id : productIds) {
            com.example.pojo.entity.Product p = productMap.get(id);
            if (p != null && p.getStatus() != null && p.getStatus() == 1) {
                result.add(ProductVO.builder()
                        .id(p.getId())
                        .userId(p.getUserId())
                        .categoryId(p.getCategoryId())
                        .title(p.getTitle())
                        .description(p.getDescription())
                        .price(p.getPrice())
                        .originalPrice(p.getOriginalPrice())
                        .condition(p.getCondition())
                        .tradeType(p.getTradeType())
                        .status(p.getStatus())
                        .viewCount(p.getViewCount())
                        .wantCount(p.getWantCount())
                        .favoriteCount(p.getFavoriteCount())
                        .createTime(p.getCreateTime())
                        .updateTime(p.getUpdateTime())
                        .build());
            }
        }
        return result;
    }

    /**
     * 为商品VO列表填充封面图片
     */
    private List<ProductVO> fillImages(List<ProductVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return vos;
        }
        List<Integer> ids = vos.stream()
                .map(ProductVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<ProductImage> images = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .in(ProductImage::getProductId, ids)
                        .orderByAsc(ProductImage::getSort)
        );

        Map<Integer, List<ProductImageVO>> imageMap = new HashMap<>();
        for (ProductImage img : images) {
            imageMap.computeIfAbsent(img.getProductId(), k -> new ArrayList<>())
                    .add(ProductImageVO.builder()
                            .id(img.getId())
                            .productId(img.getProductId())
                            .imageUrl(img.getImageUrl())
                            .isCover(img.getIsCover())
                            .sort(img.getSort())
                            .build());
        }

        for (ProductVO vo : vos) {
            vo.setImages(imageMap.getOrDefault(vo.getId(), Collections.emptyList()));
        }
        return vos;
    }
}
