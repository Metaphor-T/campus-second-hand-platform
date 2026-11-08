/**
 * 游客端 API
 *
 * @author campus-system
 */
import request from '@/utils/request'
import type { Result } from '@/types/result'
import type {
  GuestMarketData,
  ProductVO,
  CategoryVO,
  ProductReviewVO,
  ReviewStats,
  GuestProductQueryDTO,
  GuestReviewQueryDTO,
} from '@/types/guest'

/* ==================== 商品相关 ==================== */

/**
 * 获取市场页组合数据（分类 + 热门 + 商品瀑布流）
 *
 * @param params 查询参数（分类、搜索、分页）
 * @returns 市场页组合数据
 */
export function getGuestMarketData(
  params: GuestProductQueryDTO
): Promise<Result<GuestMarketData>> {
  return request.post('/guest/product/market', params)
}

/**
 * 查询商品详情
 *
 * @param id 商品ID
 * @returns 商品详情（含图册、发布者信息）
 */
export function getGuestProductDetail(id: number): Promise<Result<ProductVO>> {
  return request.get(`/guest/product/detail/${id}`)
}

/**
 * 查询热门商品
 *
 * @returns 热门商品列表
 */
export function getGuestHotProducts(): Promise<Result<ProductVO[]>> {
  return request.get('/guest/product/hot')
}

/* ==================== 分类相关 ==================== */

/**
 * 查询全部分类（用于筛选导航）
 *
 * @returns 分类列表
 */
export function getGuestCategories(): Promise<Result<CategoryVO[]>> {
  return request.get('/guest/category/list')
}

/* ==================== 评价相关 ==================== */

/**
 * 查询商品评价列表（分页）
 *
 * @param params 查询参数（productId 必填）
 * @returns 评价列表
 */
export function getGuestReviewList(
  params: GuestReviewQueryDTO
): Promise<Result<ProductReviewVO[]>> {
  return request.post('/guest/review/list', params)
}

/**
 * 查询商品评价统计
 *
 * @param productId 商品ID
 * @returns 评价统计（avgRating, reviewCount）
 */
export function getGuestReviewStats(
  productId: number
): Promise<Result<ReviewStats>> {
  return request.get(`/guest/review/stats/${productId}`)
}
