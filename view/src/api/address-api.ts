// src/api/address-api.ts 收货地址 API
import type { Result } from "@/types/result"
import type {
    AddressVO,
    AddressAddDTO,
    AddressUpdateDTO
} from "@/types/address"
import request from "@/utils/request"

/** 查询当前用户的收货地址列表 */
export const listAddressApi = () => {
    return request.get<Result<AddressVO[]>>("/address/list")
}

/** 新增收货地址 */
export const addAddressApi = (params: AddressAddDTO) => {
    return request.post<Result<void>>("/address/add", params)
}

/** 修改收货地址 */
export const updateAddressApi = (params: AddressUpdateDTO) => {
    return request.put<Result<void>>("/address/update", params)
}

/** 删除收货地址 */
export const deleteAddressApi = (id: number) => {
    return request.delete<Result<void>>(`/address/${id}`)
}

/** 设为默认地址 */
export const setDefaultAddressApi = (id: number) => {
    return request.put<Result<void>>(`/address/default/${id}`)
}
