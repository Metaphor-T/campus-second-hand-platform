// src/types/review.ts 商品评价类型文件

import type { QueryDTO } from "./base-dto"

/**
 * 商品评价视图对象
 */
export interface ProductReviewVO {
    id?: number                // 评价主键ID
    orderId?: number           // 订单ID
    productId?: number         // 商品ID
    productTitle?: string      // 商品标题
    productImage?: string      // 商品首图
    buyerId?: number           // 买家用户ID
    buyerName?: string         // 买家用户名
    buyerAvatar?: string       // 买家头像
    sellerId?: number          // 卖家用户ID
    sellerName?: string        // 卖家用户名
    rating?: number            // 评分：1-5星
    content?: string           // 评价内容
    replyContent?: string      // 卖家回复内容
    replyTime?: string         // 卖家回复时间
    status?: number            // 状态：1显示，0隐藏
    images?: string[]          // 评价图片列表
    createTime?: string        // 评价时间
    updateTime?: string        // 更新时间
}

/**
 * 新增评价参数
 */
export interface ProductReviewAddDTO {
    orderId?: number           // 订单ID
    rating?: number            // 评分：1-5星
    content?: string           // 评价内容
    images?: string[]          // 评价图片URL列表
}

/**
 * 评价查询参数
 */
export interface ProductReviewQueryDTO extends QueryDTO {
    productId?: number         // 商品ID
    sellerId?: number          // 卖家用户ID
    buyerId?: number           // 买家用户ID
    rating?: number            // 评分筛选
    status?: number            // 状态筛选：1显示，0隐藏
    hasReply?: boolean         // 是否有回复
    keyword?: string           // 关键词模糊搜索（匹配评价内容）
}

/**
 * 卖家回复评价参数
 */
export interface ProductReviewReplyDTO {
    id?: number                // 评价ID
    replyContent?: string      // 回复内容
}
