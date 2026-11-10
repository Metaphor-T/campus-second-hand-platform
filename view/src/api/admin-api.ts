// src/api/admin-api.ts
import type { Result } from "@/types/result"
import type {
    BaseUserVO,
    UserLoginDTO,
    UserLoginResponseVO,
    UserProfilePassword,
    BaseUserUpdateDTO,
    BaseUserDTO,
    BaseUser
} from "@/types/user"
import request from "@/utils/request"

// 管理员登录接口
export const loginApi = (params: UserLoginDTO) => {
    return request.post<Result<UserLoginResponseVO>>('/admin/login', params)
}

// 管理员修改密码
export const updatePasswordApi = (params: UserProfilePassword) => {
    return request.put<Result<void>>('/admin/profile/password', params)
}

// Token认证接口
export const adminTokenAuthApi = () => {
    return request.get<Result<BaseUserVO>>(`/admin/auth`)
}

// 修改信息
export const adminUpdateInfoApi = (params: Pick<BaseUserUpdateDTO, 'username' | 'phone' | 'gender'>, avatar?: File) => {
    if (!avatar) return request.put<Result<BaseUserVO>>('/admin/profile', params)
    const data = new FormData()
    data.append('profile', new Blob([JSON.stringify(params)], { type: 'application/json' }))
    data.append('avatar', avatar)
    return request.put<Result<BaseUserVO>>('/admin/profile', data)
}

// 管理员列表查询接口
export const adminListQueryApi = (params: BaseUserDTO) => {
    return request.post<Result<BaseUserVO[]>>(`/admin/listPage`, params)
}

// 管理员新增接口
export const addAdminApi = (params: BaseUser) => {
    return request.post<Result<void>>(`/admin/add`,params)
}

// 管理员修改接口
export const updateAdminApi = (params: BaseUser) => {
    return request.put<Result<void>>(`/admin/update`,params)
}

// 删除管理员
export const deleteAdminByIdApi = (baseUserId: number) => {
    return request.delete<Result<void>>(`/admin/${baseUserId}`)
}

// 通过主键ID查询管理员详情
export const detailAdminByIdApi = (baseUserId: number) => {
    return request.delete<Result<BaseUserVO>>(`/admin/${baseUserId}/detail`)
}



