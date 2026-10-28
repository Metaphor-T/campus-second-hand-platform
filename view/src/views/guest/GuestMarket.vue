<template>
    <div class="guest-market">
        <!-- ====== 🔥 热门推荐 + 分类导航 ====== -->
        <div v-if="hotProducts.length > 0" class="hot-section">
            <div class="hot-content">
                <!-- 左侧：热门轮播 -->
                <div class="hot-main" @click="onProductClick(hotProducts[activeHotIndex])">
                    <div class="hot-main-carousel">
                        <el-carousel height="100%" indicator-position="none" arrow="never" :interval="5000" ref="carouselRef" @change="onHotCarouselChange">
                            <el-carousel-item v-for="(product, i) in hotProducts" :key="i">
                                <img :src="getImageUrl(coverImage(product))" @error="onHotImgError($event)" />
                            </el-carousel-item>
                        </el-carousel>
                        <span class="hot-main-rank" :class="'rank-' + (activeHotIndex + 1)">{{ activeHotIndex + 1 }}</span>
                        <div class="hot-main-overlay">
                            <h3 class="hot-overlay-title">{{ hotProducts[activeHotIndex]?.title }}</h3>
                            <div class="hot-overlay-price">
                                <span class="hop-symbol">¥</span>
                                <span class="hop-value">{{ hotProducts[activeHotIndex]?.price }}</span>
                            </div>
                            <div class="hot-overlay-stats">
                                <span><el-icon><View /></el-icon> {{ hotProducts[activeHotIndex]?.viewCount ?? 0 }}</span>
                                <span><el-icon><Star /></el-icon> {{ hotProducts[activeHotIndex]?.favoriteCount ?? 0 }}</span>
                            </div>
                        </div>
                        <div class="hot-thumbs">
                            <div v-for="(item, idx) in hotProducts" :key="'thumb-' + idx" class="hot-thumb" :class="{ active: idx === activeHotIndex }" @click.stop="switchHot(idx)">
                                <img :src="getImageUrl(coverImage(item))" @error="onHotImgError($event)" />
                                <span class="hot-thumb-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 右侧：分类导航区 -->
                <div v-if="categories.length > 0" class="category-nav-panel">
                    <div class="cnp-header">
                        <span class="cnp-header-icon"><el-icon><Grid /></el-icon></span>
                        <span>商品分类</span>
                    </div>
                    <div class="cnp-grid">
                        <div
                            v-for="cat in categories"
                            :key="cat.id"
                            class="cnp-item"
                            :class="{ active: activeCategoryId === cat.id }"
                            @click="switchCategory(cat.id)"
                        >
                            <span class="cnp-name">{{ cat.name }}</span>
                        </div>
                    </div>
                    <div class="cnp-all" :class="{ active: activeCategoryId === undefined }" @click="switchCategory(undefined)">
                        <el-icon><Grid /></el-icon>
                        <span>查看全部</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- ====== 📦 瀑布流商品 ====== -->
        <div class="masonry-wrap" v-loading="loading && productList.length === 0">
            <template v-if="loading && productList.length === 0">
                <div v-for="n in 8" :key="'sk-' + n" class="skeleton-card" :style="{ height: 180 + (n % 3) * 80 + 'px' }">
                    <div class="sk-img"></div>
                    <div class="sk-body">
                        <div class="sk-line sk-line-short"></div>
                        <div class="sk-line sk-line-medium"></div>
                        <div class="sk-line sk-line-long"></div>
                    </div>
                </div>
            </template>
            <div ref="masonryRef" class="masonry-grid" :style="{ height: masonryHeight + 'px' }">
                <div v-for="item in productList" :key="item.id" class="masonry-item" :class="{ 'is-visible': item._visible }" :style="getItemStyle(item)" @click="onProductClick(item)">
                    <div class="product-card">
                        <div class="card-image">
                            <img :src="getImageUrl(coverImage(item))" class="card-img" loading="lazy" @load="onImageLoad(item)" @error="onImageError(item)" />
                            <span class="condition-tag" :class="'cond-' + (item.condition ?? 1)">{{ conditionLabel(item.condition) }}</span>
                            <span class="trade-badge">{{ tradeLabel(item.tradeType) }}</span>
                        </div>
                        <div class="card-body">
                            <h4 class="card-title">{{ item.title }}</h4>
                            <div class="card-price">
                                <span class="price-symbol">¥</span>
                                <span class="price">{{ item.price }}</span>
                                <span v-if="item.originalPrice" class="original-price">¥{{ item.originalPrice }}</span>
                            </div>
                            <div class="card-stats">
                                <span class="stat-item"><el-icon><View /></el-icon>{{ item.viewCount ?? 0 }}</span>
                                <span class="stat-item"><el-icon><Star /></el-icon>{{ item.favoriteCount ?? 0 }}</span>
                            </div>
                            <div class="card-meta">
                                <span class="meta-user">
                                    <el-avatar :size="16" v-if="item.userAvatar" :src="getImageUrl(item.userAvatar)" />
                                    <el-avatar :size="16" v-else>{{ (item.username ?? '?')[0] }}</el-avatar>
                                    <span class="username">{{ item.username }}</span>
                                </span>
                                <span class="meta-time">{{ formatTime(item.createTime) }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- ====== 加载更多 ====== -->
        <div ref="sentinelRef" class="load-more">
            <el-icon v-if="loadingMore" class="is-loading"><Loading /></el-icon>
            <span v-else-if="hasMore" class="load-hint">下拉加载更多</span>
            <span v-else class="no-more">— 已经到底了 —</span>
        </div>

        <!-- ====== 回到顶部 ====== -->
        <transition name="fade">
            <button v-if="showScrollTop" class="scroll-top" @click="scrollToTop"><el-icon><Top /></el-icon></button>
        </transition>
    </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, Grid, View, Star, Top } from '@element-plus/icons-vue'
import { getGuestMarketData, getGuestHotProducts, getGuestCategories } from '@/api/guest-api'
import type { ProductVO, CategoryVO } from '@/types/guest'

interface MasonryItem extends ProductVO {
    _col?: number; _top?: number; _left?: number; _width?: number; _height?: number
    _visible?: boolean; _imgLoaded?: boolean
}

const router = useRouter()
const route = useRoute()

const productList = ref<MasonryItem[]>([])
const hotProducts = ref<MasonryItem[]>([])
const activeHotIndex = ref(0)
const carouselRef = ref<any>(null)
const activeCategoryId = ref<number | undefined>(undefined)
const categories = ref<CategoryVO[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const PAGE_SIZE = 12
const searchKeyword = ref('')

// ---- Masonry ----
const masonryRef = ref<HTMLElement | null>(null)
const masonryHeight = ref(0)
const columnCount = ref(4)
const gap = 24
const columnWidth = ref(0)
let resizeTimer: ReturnType<typeof setTimeout> | null = null

const updateColumnCount = () => {
    const w = window.innerWidth
    if (w < 480) columnCount.value = 1
    else if (w < 640) columnCount.value = 2
    else if (w < 860) columnCount.value = 3
    else if (w < 1100) columnCount.value = 4
    else columnCount.value = 5
    if (productList.value.length > 0) nextTick(() => doLayout())
}

const doLayout = () => {
    if (!masonryRef.value) return
    const cw = masonryRef.value.clientWidth
    columnWidth.value = (cw - gap * (columnCount.value - 1)) / columnCount.value
    const colHeights = new Array(columnCount.value).fill(0)
    productList.value.forEach((item, idx) => {
        const col = pickColumn(colHeights)
        item._col = col; item._left = col * (columnWidth.value + gap)
        item._top = colHeights[col]; item._width = columnWidth.value
        colHeights[col] += (item._height ?? estimateCardHeight()) + gap
        setTimeout(() => { item._visible = true }, 40 + idx * 18 + Math.random() * 60)
    })
    masonryHeight.value = Math.max(...colHeights)
}

const estimateCardHeight = (): number => {
    const ratios = [0.55, 0.7, 0.85, 0.6, 1.0, 0.5, 0.75, 0.9, 0.65, 1.1, 0.58, 0.78]
    return columnWidth.value / ratios[Math.floor(Math.random() * ratios.length)] + 130 + Math.floor(Math.random() * 24)
}

const pickColumn = (colHeights: number[]): number => {
    const min = Math.min(...colHeights)
    const max = Math.max(...colHeights)
    if (max - min <= 80) {
        const candidates = colHeights.map((h, i) => ({ h, i })).filter(({ h }) => h <= min + 80)
        return candidates[Math.floor(Math.random() * candidates.length)].i
    }
    return colHeights.indexOf(min)
}

const getItemStyle = (item: MasonryItem) => ({
    position: 'absolute' as const, top: item._top + 'px', left: item._left + 'px', width: item._width + 'px'
})

const onImageLoad = (item: MasonryItem) => {
    item._imgLoaded = true
    nextTick(() => {
        if (masonryRef.value) {
            const idx = productList.value.indexOf(item)
            if (idx !== -1) {
                const el = masonryRef.value.children[idx] as HTMLElement | undefined
                if (el) { const card = el.querySelector('.product-card') as HTMLElement | null; item._height = card?.getBoundingClientRect().height ?? estimateCardHeight() }
            }
        }
        doLayout()
    })
}

const onImageError = (item: MasonryItem) => { item._imgLoaded = true; nextTick(() => doLayout()) }

// ---- 回到顶部 ----
const showScrollTop = ref(false)
let scrollThrottleTimer: ReturnType<typeof setTimeout> | null = null
const onScroll = () => {
    if (scrollThrottleTimer) return; scrollThrottleTimer = setTimeout(() => { scrollThrottleTimer = null }, 100)
    showScrollTop.value = (window.scrollY || document.documentElement.scrollTop) > 600
}
const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

// ---- 哨兵 ----
const sentinelRef = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

// ---- 工具函数 ----
const getImageUrl = (path?: string) => {
    if (!path) return ''; if (path.startsWith('http')) return path
    return `${import.meta.env.VITE_API_BASE_URL}${path}`
}
const coverImage = (item: any) => item.images?.find((i: any) => i.isCover === 1)?.imageUrl ?? item.images?.[0]?.imageUrl
const conditionLabel = (v?: number) => ({ 1: '全新', 2: '几乎全新', 3: '有使用痕迹', 4: '老旧' } as Record<number, string>)[v ?? 0] || ''
const tradeLabel = (v?: number) => ({ 1: '自提', 2: '邮寄', 3: '皆可' } as Record<number, string>)[v ?? 0] ?? ''

const formatTime = (t?: string) => {
    if (!t) return ''; const date = new Date(t); const now = new Date()
    const diff = now.getTime() - date.getTime()
    const mins = Math.floor(diff / 60000); const hrs = Math.floor(diff / 3600000); const days = Math.floor(diff / 86400000)
    if (mins < 1) return '刚刚'; if (mins < 60) return `${mins}分钟前`
    if (hrs < 24) return `${hrs}小时前`; if (days < 7) return `${days}天前`
    return date.toLocaleDateString('zh-CN')
}
// ---- 数据加载 ----
const fetchProducts = async (page: number, append: boolean, search?: string) => {
    try {
        if (!append) { productList.value = []; masonryHeight.value = 0 }
        const res = await getGuestMarketData({ current: page, size: PAGE_SIZE, categoryId: activeCategoryId.value, title: search })
        const data = res?.data as any
        const list: MasonryItem[] = (data?.productList ?? []).map((it: any) => ({ ...it, _imgLoaded: false, _visible: false }))
        const total = data?.total ?? 0
        if (append) productList.value.push(...list); else productList.value = list
        hasMore.value = productList.value.length < total
        await nextTick(); doLayout()
    } catch (e: any) { ElMessage.error(e.message || '加载失败') }
    finally { loading.value = false; loadingMore.value = false }
}

const fetchHotProducts = async () => {
    try {
        const res = await getGuestHotProducts()
        hotProducts.value = ((res?.data ?? []) as any[]).map(it => ({ ...it, _imgLoaded: false, _visible: true })) as MasonryItem[]
    } catch { /* silent */ }
}

const loadCategories = async () => {
    try { const res = await getGuestCategories(); categories.value = (res?.data ?? []).filter((c: any) => c.status !== 0) } catch { /* silent */ }
}

// ---- 交互 ----
const onHotImgError = (e: Event) => { (e.target as HTMLImageElement).style.display = 'none' }
const switchHot = (idx: number) => { activeHotIndex.value = idx; carouselRef.value?.setActiveItem(idx) }
const onHotCarouselChange = (i: number) => { activeHotIndex.value = i }

const switchCategory = (id: number | undefined) => {
    if (activeCategoryId.value === id) return
    activeCategoryId.value = id; searchKeyword.value = ''; resetPagination(); fetchProducts(1, false)
}

// ---- 商品点击 ----
const onProductClick = (item: MasonryItem) => {
    if (item.id) router.push(`/guest-home/product-detail/${item.id}`)
}

// ---- 加载更多 ----
let loadMorePending = false
const loadMore = () => {
    if (loadMorePending || loadingMore.value || !hasMore.value) return
    loadMorePending = true; loadingMore.value = true; currentPage.value += 1
    fetchProducts(currentPage.value, true).finally(() => { loadMorePending = false })
}
const resetPagination = () => { currentPage.value = 1; hasMore.value = true; loading.value = true; loadMorePending = false }

// ---- 搜索参数监听 ----
watch(() => route.query.search, (val) => {
    const kw = (val && typeof val === 'string') ? val : ''
    searchKeyword.value = kw; activeCategoryId.value = undefined; resetPagination(); fetchProducts(1, false, kw || undefined)
})

// ---- 生命周期 ----
onMounted(async () => {
    updateColumnCount()
    window.addEventListener('resize', () => { if (resizeTimer) clearTimeout(resizeTimer); resizeTimer = setTimeout(updateColumnCount, 200) })
    window.addEventListener('scroll', onScroll, { passive: true })
    await fetchHotProducts(); await loadCategories(); await fetchProducts(1, false)
    if (route.query.search && typeof route.query.search === 'string') searchKeyword.value = route.query.search
    await nextTick()
    if (sentinelRef.value) {
        observer = new IntersectionObserver((e) => { if (e[0].isIntersecting) loadMore() }, { rootMargin: '200px' })
        observer.observe(sentinelRef.value)
    }
})

onUnmounted(() => {
    if (observer) { observer.disconnect(); observer = null }
    window.removeEventListener('scroll', onScroll)
    if (resizeTimer) clearTimeout(resizeTimer)
    if (scrollThrottleTimer) clearTimeout(scrollThrottleTimer)
    window.removeEventListener('resize', () => {})
})
</script>

<style scoped lang="scss">
.guest-market { position: relative; }

.hot-section { margin-bottom: 24px; background: #fff; border-radius: 14px; }
.hot-content { display: flex; gap: 16px; min-height: 0; }
.hot-main { width: 60%; cursor: pointer; border-radius: 34px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.12); transition: box-shadow 0.3s, transform 0.3s; flex-shrink: 0; &:hover { box-shadow: 0 20px 40px rgba(0,0,0,0.2); transform: translateY(-4px); } }

/* 分类导航面板 - 校园电商绯红主题 */
.category-nav-panel {
    flex: 1; display: flex; flex-direction: column; min-height: 0;
    background: linear-gradient(160deg, #d64045 0%, #e85d3a 35%, #f07b3f 70%, #f5a623 100%);
    border-radius: 36px; padding: 22px 20px 18px;
    box-shadow: 0 4px 20px rgba(214, 64, 69, 0.35), 0 12px 48px rgba(214, 64, 69, 0.2), inset 0 1px 0 rgba(255,255,255,0.15);
    position: relative; overflow: hidden;
    &::before {
        content: ''; position: absolute; top: -60px; right: -40px;
        width: 160px; height: 160px; border-radius: 50%;
        background: radial-gradient(circle, rgba(255,255,255,0.10) 0%, transparent 70%);
        pointer-events: none;
    }
    &::after {
        content: ''; position: absolute; bottom: -30px; left: -20px;
        width: 100px; height: 100px; border-radius: 50%;
        background: radial-gradient(circle, rgba(255,255,255,0.06) 0%, transparent 70%);
        pointer-events: none;
    }
}
.cnp-header {
    display: flex; align-items: center; gap: 8px;
    font-size: 15px; font-weight: 700; color: rgba(255,255,255,0.95);
    margin-bottom: 16px; letter-spacing: 1px; position: relative; z-index: 1;
}
.cnp-header-icon {
    display: flex; align-items: center; justify-content: center;
    width: 28px; height: 28px; border-radius: 8px;
    background: rgba(255,255,255,0.2); font-size: 15px;
    backdrop-filter: blur(4px);
}
.cnp-grid {
    display: grid; grid-template-columns: 1fr 1fr; gap: 8px; flex: 1; position: relative; z-index: 1;
}
.cnp-item {
    display: flex; align-items: center; gap: 8px; padding: 10px 14px;
    border-radius: 10px; cursor: pointer; transition: all 0.25s ease;
    background: rgba(255,255,255,0.13); backdrop-filter: blur(6px);
    border: 1px solid rgba(255,255,255,0.1);
    &:hover {
        background: rgba(255,255,255,0.25);
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    }
    &.active {
        background: #fff;
        border-color: transparent;
        box-shadow: 0 4px 16px rgba(0,0,0,0.15);
        .cnp-name { color: #d64045; font-weight: 700; }
    }
}
.cnp-name {
    font-size: 13px; color: rgba(255,255,255,0.92); font-weight: 600;
    white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    transition: color 0.2s;
}
.cnp-all {
    display: flex; align-items: center; justify-content: center; gap: 6px;
    margin-top: 10px; padding: 8px 16px; border-radius: 20px; cursor: pointer;
    font-size: 13px; font-weight: 600; color: rgba(255,255,255,0.85);
    background: rgba(255,255,255,0.12); border: 1px dashed rgba(255,255,255,0.3);
    transition: all 0.25s ease; position: relative; z-index: 1;
    &:hover {
        background: rgba(255,255,255,0.22);
        border-color: rgba(255,255,255,0.5);
    }
    &.active {
        background: #fff; border-style: solid; border-color: transparent;
        color: #d64045; box-shadow: 0 4px 14px rgba(0,0,0,0.12);
    }
}
.hot-main-carousel { position: relative; width: 100%; aspect-ratio: 5/3; background: #f5f5f5; &::after { content:''; position: absolute; inset: 0; z-index: 5; box-shadow: inset 0 0 40px rgba(0,0,0,0.25); pointer-events: none; } :deep(.el-carousel), :deep(.el-carousel__container) { width: 100%; height: 100%!important; } .el-carousel__item img { width: 100%; height: 100%; object-fit: cover; display: block; } }
.hot-main-rank { position: absolute; top: 10px; left: 10px; width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 800; color: #fff; z-index: 10; box-shadow: 0 2px 8px rgba(0,0,0,0.2); &.rank-1 { background: linear-gradient(135deg,#ff6b35,#ff9500); } &.rank-2 { background: linear-gradient(135deg,#ff9500,#ffcc02); } &.rank-3 { background: linear-gradient(135deg,#ffcc02,#e8d44d); } &.rank-4,&.rank-5 { background: #aeaeb2; } }
.hot-main-overlay { position: absolute; bottom: 0; left: 0; right: 0; padding: 20px 18px 18px; background: linear-gradient(transparent,rgba(0,0,0,0.65)); z-index: 10; }
.hot-overlay-title { margin: 0 0 8px; font-size: 24px; font-weight: 600; color: #fff; line-height: 1.4; display: -webkit-box; -webkit-line-clamp:1; -webkit-box-orient: vertical; overflow: hidden; text-shadow: 0 1px 3px rgba(0,0,0,0.3); }
.hot-overlay-price { display: flex; align-items: baseline; gap: 2px; margin-bottom: 8px; .hop-symbol { font-size: 16px; font-weight: 600; color: #ffd700; } .hop-value { font-size: 28px; font-weight: 800; color: #ffd700; letter-spacing: -0.5px; text-shadow: 0 1px 4px rgba(0,0,0,0.3); } }
.hot-overlay-stats { display: flex; gap: 16px; span { display: inline-flex; align-items: center; gap: 4px; font-size: 15px; color: rgba(255,255,255,0.85); .el-icon { font-size: 16px; } } }
.hot-thumbs { position: absolute; bottom: 22px; right: 22px; display: flex; gap: 6px; z-index: 10; }
.hot-thumb { position: relative; width: 40px; height: 40px; border-radius: 6px; overflow: hidden; cursor: pointer; background: #f5f5f5; border: 2px solid rgba(255,255,255,0.5); transition: all 0.2s; flex-shrink: 0; img { width: 100%; height: 100%; object-fit: cover; display: block; } &:hover { border-color: #fff; transform: translateY(-2px); } &.active { border-color: #ffd700; } .hot-thumb-rank { position: absolute; bottom: 1px; right: 1px; width: 14px; height: 14px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 8px; font-weight: 700; color: #fff; &.rank-1 { background: linear-gradient(135deg,#ff6b35,#ff9500); } &.rank-2 { background: linear-gradient(135deg,#ff9500,#ffcc02); } &.rank-3 { background: linear-gradient(135deg,#ffcc02,#e8d44d); } &.rank-4,&.rank-5 { background: #aeaeb2; } } }

.masonry-wrap { position: relative; min-height: 400px; }
.masonry-grid { position: relative; width: 100%; transition: height 0.4s cubic-bezier(0.4,0,0.2,1); }
.masonry-item { transition: opacity 0.6s ease, transform 0.6s cubic-bezier(0.34,1.56,0.64,1); opacity: 0; transform: translateY(30px) scale(0.94); will-change: transform,opacity; &.is-visible { opacity: 1; transform: translateY(0) scale(1); } }
.product-card { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,0.04); cursor: pointer; transition: transform 0.4s cubic-bezier(0.34,1.56,0.64,1), box-shadow 0.4s; &:hover { transform: translateY(-8px) scale(1.02); box-shadow: 0 16px 40px rgba(232,93,58,0.18); } }
.card-image { position: relative; width: 100%; overflow: hidden; background: #f2f2f7; .card-img { display: block; width: 100%; height: auto; min-height: 100px; transition: transform 0.5s; &[src=''] { display: none; } } .product-card:hover & .card-img { transform: scale(1.06); } }
.condition-tag { position: absolute; top: 8px; left: 8px; padding: 2px 12px; font-size: 11px; color: #fff; border-radius: 999px; font-weight: 600; &.cond-1 { background: rgba(103,194,58,0.8); } &.cond-2 { background: rgba(64,158,255,0.8); } &.cond-3 { background: rgba(230,162,60,0.8); } &.cond-4 { background: rgba(144,147,153,0.8); } }
.trade-badge { position: absolute; top: 8px; right: 8px; padding: 2px 10px; font-size: 10px; color: #fff; background: rgba(232,93,58,0.8); border-radius: 999px; font-weight: 600; }
.card-body { padding: 10px 12px 14px; }
.card-title { margin: 0 0 8px; font-size: 14px; font-weight: 500; color: #1d1d1f; line-height: 1.45; display: -webkit-box; -webkit-line-clamp:2; -webkit-box-orient: vertical; overflow: hidden; }
.card-price { display: flex; align-items: baseline; gap: 6px; margin-bottom: 8px; .price { font-size: 18px; font-weight: 700; color: #ff6b35; letter-spacing: -0.3px; } .price-symbol { font-size: 13px; font-weight: 600; color: #ff6b35; } .original-price { font-size: 11px; color: #aeaeb2; text-decoration: line-through; } }
.card-stats { display: flex; gap: 12px; margin-bottom: 8px; .stat-item { display: inline-flex; align-items: center; gap: 3px; font-size: 11px; color: #aeaeb2; .el-icon { font-size: 12px; } } }
.card-meta { display: flex; align-items: center; justify-content: space-between; padding-top: 8px; border-top: 1px solid #f0f0f4; .meta-user { display: flex; align-items: center; gap: 4px; min-width: 0; .username { font-size: 11px; color: #86868b; max-width: 70px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } } .meta-time { font-size: 11px; color: #aeaeb2; flex-shrink: 0; } }

.skeleton-card { display: inline-block; width: calc((100% - 42px) / 4); margin: 0 7px 14px; background: #fff; border-radius: 12px; overflow: hidden; .sk-img { width: 100%; height: 60%; background: linear-gradient(90deg,#f0f0f4 25%,#e8e8ed 50%,#f0f0f4 75%); background-size: 200% 100%; animation: shimmer 1.8s infinite; } .sk-body { padding: 12px; } .sk-line { height: 10px; border-radius: 5px; background: linear-gradient(90deg,#f0f0f4 25%,#e8e8ed 50%,#f0f0f4 75%); background-size: 200% 100%; animation: shimmer 1.8s infinite; margin-bottom: 8px; &-short { width: 50%; } &-medium { width: 75%; } &-long { width: 90%; } } }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }

.load-more { display: flex; align-items: center; justify-content: center; padding: 32px 0; font-size: 13px; color: #86868b; .is-loading { font-size: 20px; color: #e85d3a; animation: rotating 1s linear infinite; } .load-hint { color: #aeaeb2; } .no-more { color: #aeaeb2; letter-spacing: 1px; } }
@keyframes rotating { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.scroll-top { position: fixed; bottom: 40px; right: 30px; z-index: 99; width: 44px; height: 44px; border-radius: 50%; border: none; background: #e85d3a; color: #fff; font-size: 18px; cursor: pointer; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 14px rgba(232,93,58,0.25); transition: all 0.3s; &:hover { background: #ff6b35; transform: translateY(-3px); box-shadow: 0 6px 20px rgba(255,107,53,0.3); } }

.fade-enter-active,.fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from,.fade-leave-to { opacity: 0; transform: scale(0.8); }

@media (max-width: 820px) { .hot-main { width: 100%; } .category-nav-panel { display: none; } }
@media (max-width: 640px) { .skeleton-card { width: calc((100% - 14px) / 2); } }
</style>
