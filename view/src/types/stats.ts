/**
 * 统计分析相关类型定义
 */

/** 概览统计卡片数据 */
export interface OverviewStats {
  totalUsers: number          // 用户总数
  totalProducts: number       // 商品总数
  totalOrders: number         // 订单总数
  totalRevenue: number        // 成交总额
  pendingReviewProducts: number // 待审核商品
  pendingRefundOrders: number   // 待退款订单
  todayNewUsers: number       // 今日新增用户
  todayNewOrders: number      // 今日新增订单
}

/** 趋势图数据项 */
export interface TrendItem {
  date: string   // 日期 yyyy-MM-dd
  count: number  // 数量
}

/** 趋势数据（折线图） */
export interface TrendsData {
  userTrends: TrendItem[]   // 用户趋势
  orderTrends: TrendItem[]  // 订单趋势
}

/** 分类销量排行项 */
export interface CategorySalesItem {
  name: string   // 分类名称
  count: number  // 销量
}

/** 订单状态分布项 */
export interface OrderStatusItem {
  status: number     // 状态码
  statusName: string // 状态名称
  count: number      // 数量
}

/** 雷达图指标数据 */
export interface RadarData {
  userActivity: number        // 用户活跃度
  productRichness: number     // 商品丰富度
  orderCompletionRate: number // 交易完成率
  positiveRate: number        // 好评率
  responseSpeed: number       // 平台响应度
}
