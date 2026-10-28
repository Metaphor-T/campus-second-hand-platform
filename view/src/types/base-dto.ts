// src/types/base-dto.ts 基础查询条件类型

// 基础查询条件类
export interface QueryDTO{
    id?: number // 主键ID
    ids?: number[] // ID列表
    current?: number // 当前页
    size?: number // 页面大小
}