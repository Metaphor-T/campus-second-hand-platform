import request from '@/utils/request'
import type { Result } from '@/types/result'
import type { AfterSaleContext } from '@/types/after-sale'

// 售后窗口集中显示一次错误；请求仍正常 reject，保留原输入。
const handled = { silentError: true }
export const getAfterSaleApi = (orderId: number) => request.get<Result<AfterSaleContext>>(`/order/after-sale/order/${orderId}`, undefined, handled)
export const applyAfterSaleApi = (orderId: number, data: { requestType: string; reason: string; returnRequired: boolean; evidence: string[] }) => request.post(`/order/after-sale/order/${orderId}`, data, handled)
export const respondAfterSaleApi = (id: number, agree: boolean, note: string) => request.put(`/order/after-sale/${id}/seller-response`, { agree, note }, handled)
export const reviewAfterSaleApi = (id: number, approved: boolean, note: string, returnRequired: boolean, originalOrderStatus?: number) => request.put(`/order/after-sale/${id}/admin-review`, { approved, note, returnRequired, originalOrderStatus }, handled)
export const confirmAfterSaleReturnApi = (id: number, note: string) => request.put(`/order/after-sale/${id}/confirm-return`, { note }, handled)
export const withdrawAfterSaleApi = (id: number) => request.put(`/order/after-sale/${id}/withdraw`, undefined, handled)
