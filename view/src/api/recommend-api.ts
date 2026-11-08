// src/api/recommend-api.ts 推荐系统 API
import type { Result } from "@/types/result"
import type { ProductVO } from "@/types/product"
import request from "@/utils/request"

/**
 * 获取个性化推荐商品列表
 * 基于物品的协同过滤算法
 * - 已登录用户：基于历史行为生成个性化推荐
 * - 未登录用户：返回热门商品
 *
 * @param size 推荐数量（默认10）
 * @returns 推荐商品列表
 */
export const recommendListApi = (size: number = 10) => {
    return request.get<Result<ProductVO[]>>("/recommend/public/list", {
        params: { size }
    })
}
