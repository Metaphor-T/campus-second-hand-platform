// src/types/product.ts 商品类型文件

import type { QueryDTO } from "./base-dto"

/**
 * 商品图片视图对象
 */
export interface ProductImageVO {
    id?: number        // 图片主键ID
    productId?: number // 商品ID
    imageUrl?: string  // 图片URL
    isCover?: number   // 是否封面：1-是，0-否
    sort?: number      // 排序
}

/**
 * 商品图片参数
 */
export interface ProductImageDTO {
    imageUrl?: string  // 图片URL地址
    isCover?: number   // 是否为封面图：1-是，0-否
    sort?: number      // 排序权重
}

/**
 * 商品视图对象
 */
export interface ProductVO {
    id?: number            // 商品主键ID
    userId?: number        // 发布用户ID
    username?: string      // 发布用户名称
    userAvatar?: string    // 发布用户头像
    categoryId?: number    // 商品分类ID
    categoryName?: string  // 分类名称
    title?: string         // 商品标题
    description?: string   // 商品描述
    price?: number         // 商品价格
    originalPrice?: number // 原价
    condition?: number     // 新旧程度：1全新，2几乎全新，3有使用痕迹，4老旧
    tradeType?: number     // 交易方式：1自提，2邮寄，3两者皆可
    status?: number        // 审核状态：0待审核，1审核通过，2审核拒绝，3已下架
    viewCount?: number     // 浏览量
    wantCount?: number     // 想要数
    favoriteCount?: number // 收藏数
    stock?: number         // 库存数量（0表示不限库存）
    version?: number       // 乐观锁版本号
    auditRemark?: string   // 审核备注
    auditTime?: string     // 审核时间
    auditorId?: number     // 审核管理员ID
    createTime?: string    // 发布时间
    updateTime?: string    // 更新时间
    images?: ProductImageVO[] // 图册列表
}

/**
 * 商品查询参数
 */
export interface ProductQueryDTO extends QueryDTO {
    userId?: number    // 用户ID
    categoryId?: number // 分类ID
    title?: string     // 标题模糊搜索
    status?: number    // 审核状态
    condition?: number // 新旧程度
    tradeType?: number // 交易方式
    minPrice?: number  // 最低价格
    maxPrice?: number  // 最高价格
}

/**
 * 新增商品参数
 */
export interface ProductAddDTO {
    categoryId?: number            // 分类ID
    title?: string                 // 商品标题
    description?: string           // 商品描述
    price?: number                 // 商品价格
    originalPrice?: number         // 原价
    condition?: number             // 新旧程度
    tradeType?: number             // 交易方式
    stock?: number            // 库存数量（0不限库存）
    images?: ProductImageDTO[]     // 图片列表
}

/**
 * 修改商品参数
 */
export interface ProductUpdateDTO {
    id?: number               // 商品ID
    categoryId?: number       // 分类ID
    title?: string            // 商品标题
    description?: string      // 商品描述
    price?: number            // 商品价格
    originalPrice?: number    // 原价
    condition?: number        // 新旧程度
    tradeType?: number        // 交易方式
    stock?: number            // 库存数量
    images?: ProductImageDTO[] // 图片列表
}

/**
 * 审核参数
 */
export interface ProductAuditDTO {
    id?: number        // 商品ID
    status?: number    // 审核结果：1通过，2拒绝
    auditRemark?: string // 审核备注
}
