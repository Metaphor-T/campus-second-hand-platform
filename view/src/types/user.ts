// src/types/user.ts 用户类型文件

import type { QueryDTO } from "./base-dto"

/**
 * 登录参数接收类
 */
export interface UserLoginDTO {
    account?: string      // 账号
    password?: string     // 密码
}

/**
 * 注册参数接收类
 */
export interface UserRegisterDTO extends UserLoginDTO {
    username?: string      // 用户名
}

/**
 * 修改密码参数类型
 */
export interface UserUpdatePasswordDTO {
    password?: string     // 密码
}

/**
 * 登录响应参数
 */
export interface UserLoginResponseVO {
    token?: string      // Token令牌
    role?: string     // 角色
}

/**
 * 基础用户信息查询条件类
 */
export interface BaseUserDTO extends QueryDTO {
    account?: string // 账号
    username?: string // 用户名
    gender?: number // 性别
    phone?: string // 手机号
    loginStatus?: number // 账号状态
}

/**
 * 基础用户信息
 */
export interface BaseUser {
    id?: number           // 主键ID
    account?: string      // 账号
    password?: string // 密码
    username?: string     // 用户名
    avatar?: string      // 头像（可选）
    gender?: number      // 性别（1：男；2：女）
    phone?: string       // 手机号（可选）
    loginStatus?: number // 账号状态（1：正常；2：封号）
    createTine?: Date // 创建时间
}


/**
 * 基础用户信息（对应后端 BaseUserVO）
 */
export interface BaseUserVO {
    id?: number           // 主键ID
    account?: string      // 账号
    username?: string     // 用户名
    avatar?: string      // 头像（可选）
    gender?: number      // 性别（1：男；2：女）
    phone?: string       // 手机号（可选）
    loginStatus?: number // 账号状态（1：正常；2：封号）
}

/**
 * 基础用户信息修改参数类型
 */
export interface BaseUserUpdateDTO {
    id?: number           // 主键ID
    username?: string     // 用户名
    avatar?: string      // 头像（可选）
    gender?: number      // 性别（1：男；2：女）
    phone?: string       // 手机号（可选）
    loginStatus?: number // 账号状态（1：正常；2：封号）
}