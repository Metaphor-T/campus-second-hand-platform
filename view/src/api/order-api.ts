// src/api/order-api.ts 订单 API
import type { Result } from "@/types/result"
import type {
    OrderVO,
    OrderCreateDTO,
    OrderQueryDTO
} from "@/types/order"
import request, { type RequestOptions } from "@/utils/request"
import type { OrderOverview } from '@/types/order'

export const getOrderOverviewApi = (orderId: number) => request.get<Result<OrderOverview>>(`/order/detail/${orderId}/overview`, undefined, { silentError: true })

/** 创建订单 */
export const createOrderApi = (params: OrderCreateDTO, options?: RequestOptions) => {
    return request.post<Result<OrderVO>>("/order/create", params, options)
}

/** 买家查询自己的订单列表（分页） */
export const listBuyerOrdersApi = (params: OrderQueryDTO, options?: RequestOptions) => {
    return request.post<Result<OrderVO[]>>("/order/buyer/list", params, options)
}

/** 卖家查询自己的订单列表（分页） */
export const listSellerOrdersApi = (params: OrderQueryDTO, options?: RequestOptions) => {
    return request.post<Result<OrderVO[]>>("/order/seller/list", params, options)
}

/** 买家取消订单 */
export const cancelOrderApi = (orderId: number, reason?: string) => {
    let url = `/order/cancel/${orderId}`
    if (reason) {
        url += `?reason=${encodeURIComponent(reason)}`
    }
    return request.put<Result<void>>(url)
}

/** 买家模拟支付 */
export const payOrderApi = (orderId: number, payMethod: number, options?: RequestOptions) => {
    return request.put<Result<void>>(`/order/pay/${orderId}?payMethod=${payMethod}`, undefined, options)
}

/** 买家确认收货 */
export const receiveOrderApi = (orderId: number) => {
    return request.put<Result<void>>(`/order/receive/${orderId}`)
}

/** 买家申请退款/售后 */
export const refundOrderApi = (orderId: number) => {
    return request.put<Result<void>>(`/order/refund/${orderId}`)
}

/** 卖家发货 */
export const shipOrderApi = (orderId: number) => {
    return request.put<Result<void>>(`/order/ship/${orderId}`)
}

/** 卖家同意退款 */
export const agreeRefundOrderApi = (orderId: number) => {
    return request.put<Result<void>>(`/order/agree-refund/${orderId}`)
}

/** 查询订单详情 */
export const getOrderDetailApi = (orderId: number) => {
    return request.get<Result<OrderVO>>(`/order/detail/${orderId}`)
}

// ==================== 管理员端 ====================

/** 管理员分页查询全部订单 */
export const listAllOrdersApi = (params: OrderQueryDTO, options?: RequestOptions) => {
    return request.post<Result<OrderVO[]>>("/order/admin/list", params, options)
}

/** 管理员同意退款审核 */
export const adminAgreeRefundApi = (orderId: number) => {
    return request.put<Result<void>>(`/order/admin/agree-refund/${orderId}`)
}

/** 查询买家各状态订单数量 */
export const countBuyerOrdersByStatusApi = () => {
    return request.get<Result<Record<number, number>>>("/order/buyer/count-by-status", undefined, { silentError: true })
}

/** 查询卖家各状态订单数量 */
export const countSellerOrdersByStatusApi = () => {
    return request.get<Result<Record<number, number>>>("/order/seller/count-by-status", undefined, { silentError: true })
}
