package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.pojo.dto.ProductReviewAddDTO;
import com.example.pojo.dto.ProductReviewQueryDTO;
import com.example.pojo.dto.ProductReviewReplyDTO;
import com.example.pojo.entity.ProductReview;
import com.example.pojo.vo.ProductReviewVO;

import java.util.List;

/**
 * 商品评价业务逻辑接口
 */
public interface ProductReviewService extends IService<ProductReview> {

    /**
     * 分页查询商品评价列表
     *
     * @param queryDTO 查询参数
     * @return 评价VO列表
     */
    List<ProductReviewVO> listPage(ProductReviewQueryDTO queryDTO);

    /**
     * 查询评价总数（配合分页）
     *
     * @param queryDTO 查询参数
     * @return 符合条件的记录数
     */
    Integer listPageCount(ProductReviewQueryDTO queryDTO);

    /**
     * 买家对已完成的订单进行评价
     *
     * @param buyerId 买家用户ID
     * @param addDTO  评价参数
     */
    void add(Integer buyerId, ProductReviewAddDTO addDTO);

    /**
     * 卖家回复评价
     *
     * @param sellerId 卖家用户ID
     * @param replyDTO 回复参数
     */
    void reply(Integer sellerId, ProductReviewReplyDTO replyDTO);

    /**
     * 管理员隐藏/显示评价
     *
     * @param id     评价ID
     * @param status 目标状态：0-隐藏，1-显示
     */
    void toggleStatus(Integer id, Integer status);

    /**
     * 查询商品评价统计（评价数 + 平均评分）
     *
     * @param productId 商品ID
     * @return { reviewCount, avgRating }
     */
    java.util.Map<String, Object> getReviewStats(Integer productId);

    /**
     * 查询当前买家是否已评价指定订单
     *
     * @param orderId 订单ID
     * @return true-已评价
     */
    boolean isReviewed(Integer orderId);
}
