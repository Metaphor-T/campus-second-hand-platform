import { ref } from 'vue'
import type { ProductQueryDTO } from '@/types/product'

export type ProductFilterValues = Pick<ProductQueryDTO, 'title' | 'minPrice' | 'maxPrice' | 'condition' | 'tradeType' | 'sortBy'>
export const MAX_PRODUCT_IMAGES = 9
export const MAX_IMAGE_BYTES = 2 * 1024 * 1024
export const PRODUCT_IMAGE_FALLBACK = 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"><rect width="400" height="300" fill="#f2f4f7"/><path d="M140 190l40-50 30 30 25-25 35 45z" fill="#8993a3"/><circle cx="245" cy="105" r="16" fill="#8993a3"/><text x="200" y="238" text-anchor="middle" font-size="20" fill="#465065">图片暂不可用</text></svg>')
export const productImageUrl = (path?: string): string => {
    if (!path) return PRODUCT_IMAGE_FALLBACK
    if (/^https?:\/\//i.test(path)) return path
    if (!path.startsWith('/files/')) return PRODUCT_IMAGE_FALLBACK
    return `${(import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')}${path}`
}
export const imageFallback = (event: Event) => {
    const image = event.target as HTMLImageElement
    if (image.src !== PRODUCT_IMAGE_FALLBACK) image.src = PRODUCT_IMAGE_FALLBACK
}
export const productErrorMessage = (error: unknown, fallback: string): string => {
    const e = error as { response?: { data?: { message?: string } }; message?: string }
    return e?.response?.data?.message || e?.message || fallback
}
export const validateProductFilters = (filters: ProductFilterValues): string => {
    if ((filters.title || '').trim().length > 200) return '关键词最多200字'
    for (const price of [filters.minPrice, filters.maxPrice]) {
        if (price != null && (!Number.isFinite(price) || price < 0 || price > 99999999.99 || Math.abs(price * 100 - Math.round(price * 100)) > 0.00001)) return '筛选价格应在0至99999999.99元之间，最多2位小数'
    }
    if (filters.minPrice != null && filters.maxPrice != null && filters.minPrice > filters.maxPrice) return '最低价格不能高于最高价格'
    return ''
}
export const validateProductImage = (file: { type: string; size: number; name: string }, count: number): string => {
    if (count >= MAX_PRODUCT_IMAGES) return '最多上传9张图片'
    if (!['image/jpeg', 'image/jpg', 'image/png'].includes(file.type) || !/\.(png|jpe?g)$/i.test(file.name)) return '仅支持JPG/PNG图片'
    if (file.size <= 0 || file.size > MAX_IMAGE_BYTES) return '图片不能为空，单张不能超过2MB'
    return ''
}
let initialLargeText = false
try { initialLargeText = localStorage.getItem('product-large-text') === 'true' } catch { /* 浏览器禁用存储时仍可使用 */ }
const largeText = ref(initialLargeText)
export const useProductReadability = () => largeText
export const setProductReadability = (enabled: boolean) => {
    largeText.value = enabled
    try { localStorage.setItem('product-large-text', String(enabled)) } catch { /* 不影响当前页 */ }
}
