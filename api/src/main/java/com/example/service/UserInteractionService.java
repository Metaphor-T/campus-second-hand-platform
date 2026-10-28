package com.example.service;

import com.example.pojo.vo.ProductVO;

import java.util.List;

/**
 * 用户交互业务逻辑接口
 * <p>处理用户对商品的收藏、想要等交互行为</p>
 */
public interface UserInteractionService {

    /**
     * 切换收藏状态（已收藏则取消，未收藏则添加）
     *
     * @param userId    当前用户ID
     * @param productId 商品ID
     * @return 操作后的收藏状态：true-已收藏，false-已取消
     */
    boolean toggleFavorite(Integer userId, Integer productId);

    /**
     * 添加"想要"记录（每个用户只能添加一次）
     *
     * @param userId    当前用户ID
     * @param productId 商品ID
     */
    void addWant(Integer userId, Integer productId);

    /**
     * 查询用户是否已收藏指定商品
     *
     * @param userId    当前用户ID
     * @param productId 商品ID
     * @return true-已收藏
     */
    boolean isFavorited(Integer userId, Integer productId);

    /**
     * 查询用户是否已"想要"指定商品
     *
     * @param userId    当前用户ID
     * @param productId 商品ID
     * @return true-已想要
     */
    boolean isWanted(Integer userId, Integer productId);

    /**
     * 查询用户收藏的商品列表（分页）
     *
     * @param userId  当前用户ID
     * @param current 页码
     * @param size    每页条数
     * @return 商品VO列表
     */
    List<ProductVO> listFavoriteProducts(Integer userId, Integer current, Integer size);

    /**
     * 查询用户收藏商品总数
     *
     * @param userId 当前用户ID
     * @return 总数
     */
    Integer listFavoriteCount(Integer userId);

    /**
     * 记录商品浏览（每次访问详情页时调用）
     * <p>插入浏览记录，并更新商品表的浏览计数字段</p>
     *
     * @param userId    当前用户ID（可为 null，表示未登录）
     * @param productId 商品ID
     */
    void recordView(Integer userId, Integer productId);
}
