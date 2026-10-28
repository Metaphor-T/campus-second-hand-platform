// src/api/product-api.ts 商品 API
import type { Result } from "@/types/result"
import type {
    ProductVO,
    ProductQueryDTO,
    ProductAddDTO,
    ProductUpdateDTO,
    ProductAuditDTO
} from "@/types/product"
import request from "@/utils/request"

// ==================== 管理员端 ====================

/** 管理员分页查询所有商品 */
export const productListPageApi = (params: ProductQueryDTO) => {
    return request.post<Result<ProductVO[]>>("/product/listPage", params)
}

/** 管理员审核商品 */
export const auditProductApi = (params: ProductAuditDTO) => {
    return request.put<Result<void>>("/product/audit", params)
}

/** 管理员删除商品 */
export const deleteProductByIdApi = (id: number) => {
    return request.delete<Result<void>>(`/product/${id}`)
}

// ==================== 用户端 ====================

/** 用户查询自己发布的商品 */
export const myProductListApi = (params: ProductQueryDTO) => {
    return request.post<Result<ProductVO[]>>("/product/myProducts", params)
}

/** 用户发布商品 */
export const addProductApi = (params: ProductAddDTO) => {
    return request.post<Result<void>>("/product/add", params)
}

/** 用户修改商品 */
export const updateProductApi = (params: ProductUpdateDTO) => {
    return request.put<Result<void>>("/product/update", params)
}

/** 用户下架商品 */
export const downProductApi = (id: number) => {
    return request.put<Result<void>>(`/product/down/${id}`)
}

/** 用户重新上架商品 */
export const upProductApi = (id: number) => {
    return request.put<Result<void>>(`/product/up/${id}`)
}

/** 用户删除商品 */
export const deleteMyProductApi = (id: number) => {
    return request.delete<Result<void>>(`/product/my/${id}`)
}

// ==================== 公开端 ====================

/** 公开的商品列表（仅展示审核通过的商品，用于前端商品市场） */
export const publicProductListApi = (params: ProductQueryDTO) => {
    return request.post<Result<ProductVO[]>>("/product/public/list", params)
}

/** 查询单个商品详情（联表 user、category，含图册） */
export const getProductDetailApi = (id: number) => {
    return request.get<Result<ProductVO>>(`/product/public/detail/${id}`)
}

// ==================== 收藏/想要 ====================

/** 切换收藏状态 */
export const toggleFavoriteApi = (productId: number) => {
    return request.post<Result<{ favorited: boolean }>>(`/product/favorite/toggle/${productId}`)
}

/** 添加"想要"记录 */
export const addWantApi = (productId: number) => {
    return request.post<Result<void>>(`/product/want/${productId}`)
}

/** 查询当前用户对指定商品的交互状态（收藏/想要） */
export const getInteractStatusApi = (productId: number) => {
    return request.get<Result<{ favorited: boolean; wanted: boolean }>>(`/product/interact/status/${productId}`)
}

/** 查询当前用户收藏的商品列表（分页） */
export const listFavoritesApi = (params: ProductQueryDTO) => {
    return request.post<Result<ProductVO[]>>("/product/favorite/list", params)
}

// ==================== 浏览记录 ====================

// ==================== 热门商品 ====================

/** 查询热门商品列表（前5条），用于首页商品类别上方展示 */
export const hotProductListApi = () => {
    return request.get<Result<ProductVO[]>>("/product/public/hot")
}

/** 记录商品浏览 */
export const recordViewApi = (productId: number) => {
    return request.post<Result<void>>(`/product/view/${productId}`)
}
