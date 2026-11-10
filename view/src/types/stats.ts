export interface OverviewStats {
  users: number; products: number; orders: number; completedAmount: number
  pendingProducts: number; refundOrders: number
  todayUsers: number; todayProducts: number; todayOrders: number
}
export interface PeriodStats {
  users: number; products: number; orders: number; completedOrders: number
  completedQuantity: number; completedAmount: number
}
export interface DailyStats { date: string; users: number; products: number; orders: number }
export interface CategoryStats {
  id: number; name: string; products: number; productShare: number | null
  completedOrders: number; completedQuantity: number; quantityShare: number | null; completedAmount: number
}
export interface OrderStatusStats { status: number; name: string; count: number; share: number | null }
export interface RatioStats { key: string; name: string; numerator: number; denominator: number; percent: number | null; definition: string }
export interface DashboardStats {
  generatedAt: string; timezone: string; today: string; startDate: string; endDate: string
  overview: OverviewStats; period: PeriodStats; trends: DailyStats[]; categories: CategoryStats[]
  orderStatus: OrderStatusStats[]; ratios: RatioStats[]
}
