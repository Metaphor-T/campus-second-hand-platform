// src/api/review-api.ts 商品评价 API
import type { Result } from "@/types/result"
import type {
    ProductReviewVO,
    ProductReviewAddDTO,
    ProductReviewQueryDTO,
    ProductReviewReplyDTO
} from "@/types/review"
import request from "@/utils/request"

// ==================== 公开接口 ====================

/** 查询某个商品的评价列表（分页，仅显示审核通过的评价） */
export const publicReviewListApi = (params: ProductReviewQueryDTO) => {
    return request.post<Result<ProductReviewVO[]>>("/review/public/list", params)
}

/** 查询商品评价统计（评价数 + 平均评分） */
export const getReviewStatsApi = (productId: number) => {
    return request.get<Result<{ avgRating: number; reviewCount: number }>>(`/review/public/stats/${productId}`)
}

// ==================== 买家接口 ====================

/** 买家对已完成的订单进行评价 */
export const addReviewApi = (params: ProductReviewAddDTO) => {
    return request.post<Result<void>>("/review/add", params)
}

/** 查询当前买家写的评价列表（分页） */
export const myReviewListApi = (params: ProductReviewQueryDTO) => {
    return request.post<Result<ProductReviewVO[]>>("/review/myReviews", params)
}

/** 查询订单是否已评价 */
export const checkReviewedApi = (orderId: number) => {
    return request.get<Result<{ reviewed: boolean }>>(`/review/check/${orderId}`)
}

// ==================== 卖家接口 ====================

/** 卖家回复评价 */
export const replyReviewApi = (params: ProductReviewReplyDTO) => {
    return request.post<Result<void>>("/review/reply", params)
}

/** 查询当前卖家收到的评价列表（分页） */
export const sellerReviewListApi = (params: ProductReviewQueryDTO) => {
    return request.post<Result<ProductReviewVO[]>>("/review/seller/reviews", params)
}

// ==================== 管理员接口 ====================

/** 管理员分页查询所有评价 */
export const reviewListPageApi = (params: ProductReviewQueryDTO) => {
    return request.post<Result<ProductReviewVO[]>>("/review/listPage", params)
}

/** 管理员隐藏/显示评价 */
export const toggleReviewStatusApi = (id: number, status: number) => {
    return request.put<Result<void>>("/review/toggleStatus", { id, status })
}
