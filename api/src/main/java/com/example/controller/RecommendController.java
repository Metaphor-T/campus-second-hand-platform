package com.example.controller;

import com.example.annotation.Auth;
import com.example.pojo.api.Result;
import com.example.pojo.vo.ProductVO;
import com.example.service.RecommendService;
import com.example.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐系统控制器
 * <p>提供基于物品协同过滤的个性化推荐接口</p>
 * <ul>
 *   <li>已登录用户：基于历史行为生成个性化推荐</li>
 *   <li>未登录用户/冷启动：返回热门商品</li>
 *   <li>管理员可手动触发相似度矩阵重算</li>
 * </ul>
 */
@RestController
@RequestMapping(value = "/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    /**
     * 获取个性化推荐商品列表
     * <p>基于物品的协同过滤算法，登录用户返回个性化推荐，未登录返回热门商品</p>
     *
     * @param size 需要返回的商品数量（默认10）
     * @return 推荐商品列表
     */
    @Auth
    @GetMapping(value = "/public/list")
    public Result<List<ProductVO>> recommendList(
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Integer userId = UserContext.getUserId();
        List<ProductVO> list = recommendService.recommendForUser(userId, size);
        return Result.success(list);
    }

    /**
     * 主动刷新推荐结果（忽略缓存，实时计算）
     *
     * @param size 需要返回的商品数量（默认10）
     * @return 推荐商品列表
     */
    @Auth
    @GetMapping(value = "/public/refresh")
    public Result<List<ProductVO>> refreshRecommend(
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Integer userId = UserContext.getUserId();
        List<ProductVO> list = recommendService.refreshRecommend(userId, size);
        return Result.success(list);
    }

    /**
     * 手动触发全量相似度矩阵重算（仅管理员可用）
     *
     * @return 操作结果
     */
    @Auth
    @PostMapping(value = "/recompute")
    public Result<String> recomputeSimilarities() {
        // 异步触发，避免阻塞请求
        new Thread(recommendService::recomputeSimilarities).start();
        return Result.success("相似度重算任务已提交，将在后台执行");
    }

    /**
     * 获取热门商品（已有接口的别名，便于前端统一调用）
     *
     * @return 热门商品列表（最多5条）
     */
    @Auth
    @GetMapping(value = "/public/hot")
    public Result<List<ProductVO>> hotList() {
        // 委托给 RecommendService，冷启动场景同样适用
        List<ProductVO> list = recommendService.recommendForUser(null, 5);
        return Result.success(list);
    }
}
