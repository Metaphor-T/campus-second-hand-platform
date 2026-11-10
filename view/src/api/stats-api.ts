import { get } from '@/utils/request'
import type { Result } from '@/types/result'
import type { DashboardStats } from '@/types/stats'

export const getDashboardStats = (range?: { startDate: string; endDate: string }) =>
  get<Result<DashboardStats>>('/stats/dashboard', range)
