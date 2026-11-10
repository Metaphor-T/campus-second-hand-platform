import type { UserProfile, UserProfileUpdate, UserProfilePassword } from '@/types/user'
// src/api/user-api.ts
import type { Result } from "@/types/result"
import type {
    BaseUserVO,
    UserLoginDTO,
    UserLoginResponseVO,
    BaseUserUpdateDTO,
    BaseUserDTO,
    BaseUser,
    UserRegisterDTO
} from "@/types/user"
import request from "@/utils/request"

// 用户登录接口
export const userLoginApi = (params: UserLoginDTO) => {
    return request.post<Result<UserLoginResponseVO>>('/user/login', params)
}

// 用户注册接口
export const registerApi = (params: UserRegisterDTO) => {
    return request.post<Result<UserLoginResponseVO>>('/user/register', params)
}

// 兼容旧地址；同样要求原密码、新密码和确认密码。新页面使用 changeMyPasswordApi。
export const updatePasswordApi = (id: number,params: UserProfilePassword) => {
    return request.put<Result<void>>(`/user/${id}/updatePassword`, params)
}

// Token认证接口
export const userTokenAuthApi = () => {
    return request.get<Result<BaseUserVO>>(`/user/auth`)
}

// 修改信息
export const userUpdateInfoApi = (params: BaseUserUpdateDTO) => {
    return request.put<Result<BaseUserVO>>(`/user`,params)
}

// 旧接口已停用；个人中心通过 saveMyProfileApi 将头像与资料统一保存。
export const uploadAvatarApi = (params: BaseUserUpdateDTO) => {
    return request.put<Result<BaseUserVO>>(`/user/uploadAvatar`,params)
}

// 用户列表查询接口
export const userListQueryApi = (params: BaseUserDTO) => {
    return request.post<Result<BaseUserVO[]>>(`/user/listPage`, params)
}

// 用户新增接口
export const adduserApi = (params: BaseUser) => {
    return request.post<Result<void>>(`/user/add`,params)
}

// 用户修改接口
export const updateuserApi = (params: BaseUser) => {
    return request.put<Result<void>>(`/user/update`,params)
}

// 删除用户
export const deleteuserByIdApi = (baseUserId: number) => {
    return request.delete<Result<void>>(`/user/${baseUserId}`)
}

// 通过主键ID查询用户详情
export const detailuserByIdApi = (baseUserId: number) => {
    return request.delete<Result<BaseUserVO>>(`/user/${baseUserId}/detail`)
}
export const getMyProfileApi = () => request.get<Result<UserProfile>>('/user/profile')
export const saveMyProfileApi = (profile: UserProfileUpdate, avatar?: File) => {
    if (!avatar) return request.put<Result<UserProfile>>('/user/profile', profile)
    const data = new FormData()
    data.append('profile', new Blob([JSON.stringify(profile)], { type: 'application/json' }))
    data.append('avatar', avatar)
    return request.put<Result<UserProfile>>('/user/profile', data)
}
export const changeMyPasswordApi = (password: UserProfilePassword) => request.put<Result<void>>('/user/profile/password', password)
