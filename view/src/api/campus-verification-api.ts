import request from '@/utils/request'
import type { Result } from '@/types/result'
import type { CampusVerification } from '@/types/campus-verification'

const url = '/user/profile/campus-verification'
export const getCampusVerificationApi = () => request.get<Result<CampusVerification>>(url)
export const applyCampusVerificationApi = () => request.post<Result<CampusVerification>>(url, { consent: true })
export const refreshCampusVerificationApi = () => request.post<Result<CampusVerification>>(url + '/refresh')
