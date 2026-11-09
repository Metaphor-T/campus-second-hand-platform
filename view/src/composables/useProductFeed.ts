import { ref, shallowRef, type Ref } from 'vue'

/** 页码仅在成功时推进；换筛选条件会使旧请求失效；失败保留已有商品。 */
export function useProductFeed<T>(fetchPage: (page: number, query: Record<string, unknown>) => Promise<{ items: T[]; total: number }>) {
    const items = ref<T[]>([]) as Ref<T[]>
    const loading = ref(false)
    const loadingMore = ref(false)
    const hasMore = ref(true)
    const currentPage = ref(0)
    const error = shallowRef<unknown>(null)
    let generation = 0
    let disposed = false
    let failed: { page: number; append: boolean; query: Record<string, unknown> } | null = null
    async function request(page: number, append: boolean, query: Record<string, unknown>): Promise<boolean> {
        if (disposed || (append && (loading.value || loadingMore.value || !hasMore.value))) return false
        if (!append) { generation++; loading.value = true; loadingMore.value = false }
        else loadingMore.value = true
        const token = generation
        error.value = null
        failed = null
        try {
            const result = await fetchPage(page, { ...query })
            if (disposed || token !== generation) return false
            // 刷新或筛选失败之前保留旧结果；成功后原子替换。
            items.value = append ? [...items.value, ...result.items] : result.items
            currentPage.value = page
            hasMore.value = items.value.length < result.total && result.items.length > 0
            return true
        } catch (e) {
            if (disposed || token !== generation) return false
            error.value = e
            failed = { page, append, query: { ...query } }
            return false
        } finally {
            if (!disposed && token === generation) { loading.value = false; loadingMore.value = false }
        }
    }
    const retry = () => failed ? request(failed.page, failed.append, failed.query) : Promise.resolve(false)
    const dispose = () => { disposed = true; generation++ }
    return { items, loading, loadingMore, hasMore, currentPage, error, request, retry, dispose }
}
