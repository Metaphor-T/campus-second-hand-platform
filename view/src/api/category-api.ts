// src/api/category-api.ts 商品分类 API
import type { Result } from "@/types/result"
import type { CategoryQueryDTO, CategoryAddDTO, CategoryUpdateDTO, CategoryVO, CategorySortDTO } from "@/types/category"
import request from "@/utils/request"

/**
 * 分页查询商品分类列表
 */
export const categoryListQueryApi = (params: CategoryQueryDTO) => {
    return request.post<Result<CategoryVO[]>>("/category/listPage", params)
}

/**
 * 查询所有商品分类（扁平列表，用于前端构建树形结构）
 */
export const categoryListAllApi = () => {
    return request.get<Result<CategoryVO[]>>("/category/listAll")
}

/**
 * 批量更新商品分类排序（拖拽排序后调用）
 */
export const updateCategorySortApi = (params: CategorySortDTO[]) => {
    return request.put<Result<void>>("/category/updateSort", params)
}

/**
 * 新增商品分类
 */
export const addCategoryApi = (params: CategoryAddDTO) => {
    return request.post<Result<void>>("/category/add", params)
}

/**
 * 修改商品分类
 */
export const updateCategoryApi = (params: CategoryUpdateDTO) => {
    return request.put<Result<void>>("/category/update", params)
}

/**
 * 删除商品分类
 */
export const deleteCategoryByIdApi = (id: number) => {
    return request.delete<Result<void>>(`/category/${id}`)
}
