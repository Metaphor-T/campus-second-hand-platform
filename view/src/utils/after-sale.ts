import type { AfterSaleRequest } from '@/types/after-sale'
import type { OrderVO } from '@/types/order'
export const afterSaleLabel = (state?: string) => ({ WAIT_SELLER: '等待卖家回应', WAIT_ADMIN: '等待管理员审核', WAIT_RETURN: '审核通过，等待退回商品', APPROVED: '售后已完成', REJECTED: '售后申请已驳回', WITHDRAWN: '售后申请已撤回' }[state ?? ''] || '未申请售后')
export const activeAfterSale = (r?: AfterSaleRequest) => !!r && ['WAIT_SELLER', 'WAIT_ADMIN', 'WAIT_RETURN'].includes(r.state)
export const afterSaleEntry = (order: OrderVO) => order.afterSale ? '查看售后' : order.afterSaleEligibility === 'ADMIN_ONLY' ? '申请管理员介入' : '申请售后'
export const inventoryResult = (r: AfterSaleRequest) => ({ CREDITED: `已恢复可售库存 ${r.stockCreditedQuantity ?? 0} 件`, UNLIMITED_RECORDED: '退回数量已记录；当前为不限库存，不累加数量', MODE_UNCONFIRMED_RECORDED: '库存模式待确认，退回数量已记录，请卖家核对可售数量', LIMIT_RECORDED: '可售数量已达上限，退回数量已记录，请卖家核对库存', UNKNOWN_DEDUCTION: '历史扣库记录不明，退回数量已记录，请管理员与卖家核对库存', PRODUCT_MISSING_RECORDED: '原商品已不存在，退回数量已记录', NO_DEDUCTION: '原订单未扣减库存数量，不增加可售库存', NO_RETURN: '本次无需退回商品，不恢复库存' }[r.stockDisposition ?? ''] || '')
