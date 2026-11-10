export type CampusVerificationStatus = 'UNVERIFIED' | 'PENDING' | 'VERIFIED' | 'REJECTED' | 'INVALIDATED' | 'EXPIRED'
export interface CampusVerification {
  available: boolean
  status: CampusVerificationStatus
  message: string
  school: string | null
  maskedStudentNumber: string | null
  requestedAt: string | null
  checkedAt: string | null
  expiresAt: string | null
}
