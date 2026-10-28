// src/types/category.ts 商品分类类型文件

import type { QueryDTO } from "./base-dto"

/**
 * 商品分类查询条件类
 */
export interface CategoryQueryDTO extends QueryDTO {
    name?: string       // 分类名称（模糊搜索）
    parentId?: number   // 父分类ID
    status?: number     // 状态：1-启用，0-禁用
}

/**
 * 新增商品分类参数
 */
export interface CategoryAddDTO {
    parentId?: number   // 父分类ID，0表示顶级分类
    name?: string       // 分类名称
    sort?: number       // 排序权重
    status?: number     // 状态：1-启用，0-禁用
}

/**
 * 修改商品分类参数
 */
export interface CategoryUpdateDTO {
    id?: number         // 分类主键ID
    parentId?: number   // 父分类ID
    name?: string       // 分类名称
    sort?: number       // 排序权重
    status?: number     // 状态：1-启用，0-禁用
}

/**
 * 排序参数（拖拽排序后提交）
 */
export interface CategorySortDTO {
    id: number          // 分类主键ID
    parentId: number    // 父分类ID
    sort: number        // 排序权重
    level?: number      // 层级深度
}

/**
 * 商品分类视图对象
 */
export interface CategoryVO {
    id?: number            // 分类主键ID
    parentId?: number      // 父分类ID
    name?: string          // 分类名称
    sort?: number          // 排序权重
    status?: number        // 状态：1-启用，0-禁用
    createTime?: string    // 创建时间
    children?: CategoryVO[] // 子分类列表（前端构建树结构使用）
    _level?: number        // 层级深度（前端使用）
}
