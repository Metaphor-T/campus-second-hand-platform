import request, { get } from '@/utils/request'
import type { Result } from '@/types/result'
import type { OverviewStats, TrendsData, CategorySalesItem, OrderStatusItem, RadarData } from '@/types/stats'

/**
 * 统计分析 API
 * 注意：后端返回 Result<T> 包装，拦截器不做二次解包，故泛型需包裹 Result<T>
 */

/** 获取概览统计卡片数据 */
export const getOverviewStats = () =>
  get<Result<OverviewStats>>('/stats/overview')

/** 获取趋势图数据（折线图） */
export const getTrends = (days: number = 7) =>
  get<Result<TrendsData>>('/stats/trends', { days })

/** 获取分类销量排行（柱状图） */
export const getCategorySales = () =>
  get<Result<CategorySalesItem[]>>('/stats/category-sales')

/** 获取订单状态分布（饼状图） */
export const getOrderStatus = () =>
  get<Result<OrderStatusItem[]>>('/stats/order-status')

/** 获取平台健康雷达图数据 */
export const getRadarData = () =>
  get<Result<RadarData>>('/stats/radar')
