package com.example.controller;

import com.example.annotation.Auth;
import com.example.annotation.Pager;
import com.example.pojo.api.Result;
import com.example.pojo.dto.ProductReviewAddDTO;
import com.example.pojo.dto.ProductReviewQueryDTO;
import com.example.pojo.dto.ProductReviewReplyDTO;
import com.example.pojo.enums.RoleEnum;
import com.example.pojo.vo.ProductReviewVO;
import com.example.service.ProductReviewService;
import com.example.utils.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品评价控制器
 * <p>买家：发表评价</p>
 * <p>卖家：回复评价</p>
 * <p>管理员：管理评价</p>
 */
@RestController
@RequestMapping(value = "/review")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    // ==================== 公开接口（无需严格登录） ====================

    /**
     * 查询某个商品的评价列表（分页，仅显示审核通过的评价）
     *
     * @param queryDTO 查询参数（productId 必填）
     * @return 分页列表
     */
    @Pager
    @PostMapping(value = "/public/list")
    public Result<List<ProductReviewVO>> publicList(@RequestBody ProductReviewQueryDTO queryDTO) {
        // 公开接口只展示状态为「显示」的评价
        queryDTO.setStatus(1);
        List<ProductReviewVO> list = productReviewService.listPage(queryDTO);
        Integer count = productReviewService.listPageCount(queryDTO);
        return Result.success(list, count);
    }

    /**
     * 查询商品评价统计（评价数 + 平均评分）
     *
     * @param productId 商品ID
     * @return { avgRating, reviewCount }
     */
    @GetMapping(value = "/public/stats/{productId}")
    public Result<Map<String, Object>> getReviewStats(@PathVariable Integer productId) {
        Map<String, Object> stats = productReviewService.getReviewStats(productId);
        return Result.success(stats);
    }

    // ==================== 买家接口 ====================

    /**
     * 买家对已完成的订单进行评价
     *
     * @param addDTO 评价参数（含订单ID、评分、内容、图片）
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PostMapping(value = "/add")
    public Result<Void> add(@Valid @RequestBody ProductReviewAddDTO addDTO) {
        Integer buyerId = UserContext.getUserId();
        productReviewService.add(buyerId, addDTO);
        return Result.success();
    }

    /**
     * 查询当前买家写的评价列表（分页）
     *
     * @param queryDTO 查询参数
     * @return 分页列表
     */
    @Auth({RoleEnum.USER})
    @Pager
    @PostMapping(value = "/myReviews")
    public Result<List<ProductReviewVO>> myReviews(@RequestBody ProductReviewQueryDTO queryDTO) {
        queryDTO.setBuyerId(UserContext.getUserId());
        List<ProductReviewVO> list = productReviewService.listPage(queryDTO);
        Integer count = productReviewService.listPageCount(queryDTO);
        return Result.success(list, count);
    }

    /**
     * 查询订单是否已评价（用于前端判断是否显示评价按钮）
     *
     * @param orderId 订单ID
     * @return { reviewed: boolean }
     */
    @Auth({RoleEnum.USER})
    @GetMapping(value = "/check/{orderId}")
    public Result<Map<String, Object>> checkReviewed(@PathVariable Integer orderId) {
        boolean reviewed = productReviewService.isReviewed(orderId);
        return Result.success(Map.of("reviewed", reviewed));
    }

    // ==================== 卖家接口 ====================

    /**
     * 卖家回复评价
     *
     * @param replyDTO 回复参数（评价ID + 回复内容）
     * @return 操作结果
     */
    @Auth({RoleEnum.USER})
    @PostMapping(value = "/reply")
    public Result<Void> reply(@Valid @RequestBody ProductReviewReplyDTO replyDTO) {
        Integer sellerId = UserContext.getUserId();
        productReviewService.reply(sellerId, replyDTO);
        return Result.success();
    }

    /**
     * 查询当前卖家收到的评价列表（分页）
     *
     * @param queryDTO 查询参数
     * @return 分页列表
     */
    @Auth({RoleEnum.USER})
    @Pager
    @PostMapping(value = "/seller/reviews")
    public Result<List<ProductReviewVO>> sellerReviews(@RequestBody ProductReviewQueryDTO queryDTO) {
        queryDTO.setSellerId(UserContext.getUserId());
        List<ProductReviewVO> list = productReviewService.listPage(queryDTO);
        Integer count = productReviewService.listPageCount(queryDTO);
        return Result.success(list, count);
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员分页查询所有评价
     *
     * @param queryDTO 查询参数
     * @return 分页列表
     */
    @Auth({RoleEnum.ADMIN})
    @Pager
    @PostMapping(value = "/listPage")
    public Result<List<ProductReviewVO>> listPage(@RequestBody ProductReviewQueryDTO queryDTO) {
        List<ProductReviewVO> list = productReviewService.listPage(queryDTO);
        Integer count = productReviewService.listPageCount(queryDTO);
        return Result.success(list, count);
    }

    /**
     * 管理员隐藏/显示评价
     *
     * @param param { id, status }
     * @return 操作结果
     */
    @Auth({RoleEnum.ADMIN})
    @PutMapping(value = "/toggleStatus")
    public Result<Void> toggleStatus(@RequestBody Map<String, Integer> param) {
        Integer id = param.get("id");
        Integer status = param.get("status");
        productReviewService.toggleStatus(id, status);
        return Result.success();
    }
}
