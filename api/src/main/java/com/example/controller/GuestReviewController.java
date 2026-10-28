package com.example.controller;

import com.example.annotation.Pager;
import com.example.pojo.api.Result;
import com.example.pojo.dto.ProductReviewQueryDTO;
import com.example.pojo.vo.ProductReviewVO;
import com.example.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 游客端 - 评价浏览控制器
 * <p>提供游客无需登录即可浏览的公开评价相关接口</p>
 *
 * @author campus-system
 */
@RestController
@RequestMapping("/guest/review")
@RequiredArgsConstructor
public class GuestReviewController {

    private final ProductReviewService reviewService;

    /**
     * 查询某商品的评价列表（分页）
     * <p>仅展示通过审核的评价（status=1），按创建时间倒序</p>
     *
     * @param queryDTO 查询参数（productId必填，支持分页）
     * @return 评价分页列表
     */
    @PostMapping("/list")
    @Pager
    public Result<List<ProductReviewVO>> listByProduct(@RequestBody ProductReviewQueryDTO queryDTO) {
        List<ProductReviewVO> list = reviewService.listPage(queryDTO);
        return Result.success(list);
    }

    /**
     * 查询某商品的评价统计
     * <p>返回平均评分和评价总数</p>
     *
     * @param productId 商品ID
     * @return 评价统计数据（avgRating, reviewCount）
     */
    @GetMapping("/stats/{productId}")
    public Result<Map<String, Object>> getReviewStats(@PathVariable Integer productId) {
        Map<String, Object> stats = reviewService.getReviewStats(productId);
        return Result.success(stats);
    }
}
