package com.example.service;

import com.example.pojo.vo.ProductVO;

import java.util.List;

/**
 * 推荐系统业务逻辑接口
 * <p>基于物品的协同过滤推荐算法，冷启动阶段使用热门商品替代</p>
 */
public interface RecommendService {

    /**
     * 为用户生成个性化推荐商品列表
     * <p>算法步骤：</p>
     * <ol>
     *   <li>查询用户行为历史 → 构建用户-物品评分向量</li>
     *   <li>遍历用户交互过的商品，从 item_similarity 表中获取最相似的候选商品</li>
     *   <li>排除用户已交互过的商品，加权聚合候选得分</li>
     *   <li>按预测得分降序排序，取前 N 条返回</li>
     * </ol>
     * <p>冷启动处理：若用户无历史行为，则返回热门商品（由 ProductService 提供）</p>
     *
     * @param userId 当前用户ID（为 null 时直返热门商品）
     * @param size   需要返回的商品数量
     * @return 推荐商品列表（含封面图册）
     */
    List<ProductVO> recommendForUser(Integer userId, int size);

    /**
     * 主动刷新热门商品推荐（用于"猜你喜欢"等场景）
     * <p>与 recommendForUser 的区别：</p>
     * <ul>
     *   <li>仅使用收藏/想要/下单/评价等强信号行为，排除浏览</li>
     *   <li>结果不含用户已交互商品</li>
     * </ul>
     *
     * @param userId 当前用户ID
     * @param size   需要返回的商品数量
     * @return 推荐商品列表
     */
    List<ProductVO> refreshRecommend(Integer userId, int size);

    /**
     * 定时任务：全量重算商品相似度矩阵（基于物品的协同过滤）
     * <p>步骤：</p>
     * <ol>
     *   <li>从四个行为表 UNION ALL 查询全量用户行为</li>
     *   <li>按用户分组，构建 用户×商品 评分矩阵（加权汇总各行为分值）</li>
     *   <li>对每对有共同用户的商品对计算余弦相似度</li>
     *   <li>清空 item_similarity 表，批量插入新的相似度记录</li>
     * </ol>
     * <p>建议执行频率：每小时一次（可通过 @Scheduled 或手动调用触发）</p>
     */
    void recomputeSimilarities();
}
