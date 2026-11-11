export type AfterSaleState = 'WAIT_SELLER' | 'WAIT_ADMIN' | 'WAIT_RETURN' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN'
export interface AfterSaleRequest {
  id: number; orderId: number; requestType: 'REGULAR' | 'ADMIN_INTERVENTION' | 'LEGACY'; state: AfterSaleState
  reason: string; evidenceJson?: string; originalOrderStatus?: number; returnRequired: boolean
  applicantId?: number; createdAt?: string; updatedAt?: string
  sellerAgree?: boolean; sellerNote?: string; sellerId?: number; sellerRespondedAt?: string
  adminId?: number; adminNote?: string; adminReviewedAt?: string
  returnConfirmedBy?: number; returnNote?: string; returnConfirmedAt?: string
  returnQuantity?: number; stockCreditedQuantity?: number; stockDisposition?: string
}
export interface AfterSaleContext {
  orderId: number; orderStatus: number; quantity: number; viewerRole: 'BUYER' | 'SELLER' | 'ADMIN'
  eligibility: 'REGULAR' | 'ADMIN_ONLY' | 'PROCESSING' | 'UNAVAILABLE'; eligibilityMessage: string
  deadline?: string; serverTime: string; requests: AfterSaleRequest[]
}
