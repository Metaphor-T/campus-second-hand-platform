// src/api/result-api.ts 后端通用返回封装类型

// 后端通用返回封装类型
export interface Result<T>{
    code: number // 响应码
    message: string // 响应消息
    data?: T // 数据 （可选）
    count?: number // 数据量（可选）
    timestamp: number // 时间戳
}