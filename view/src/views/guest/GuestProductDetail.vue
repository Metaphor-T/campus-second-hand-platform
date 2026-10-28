<template>
    <div class="product-detail" v-loading="loading">
        <div class="back-bar" @click="goBack">
            <el-icon :size="20"><ArrowLeft /></el-icon>
            <span>{{ product?.title ?? '商品' }}详情</span>
        </div>

        <template v-if="product">
            <!-- ====== 商品信息 ====== -->
            <div class="info-section">
                <div class="info-header">
                    <h2 class="product-title">{{ product.title }}</h2>
                    <div class="price-row">
                        <span class="price-amount">
                            <span class="price-symbol">¥</span>
                            <span class="price">{{ product.price }}</span>
                        </span>
                        <span v-if="product.originalPrice" class="original-price">原价 ¥{{ product.originalPrice }}</span>
                    </div>
                </div>

                <div class="tag-row">
                    <span class="tag-item" :class="'tag-' + conditionType(product.condition)">
                        <el-icon :size="13"><Medal /></el-icon>{{ conditionLabel(product.condition) }}
                    </span>
                    <span class="tag-item tag-trade">
                        <el-icon :size="13"><Van /></el-icon>{{ tradeTypeLabel(product.tradeType) }}
                    </span>
                    <span v-if="product.categoryName" class="tag-item tag-category">
                        <el-icon :size="13"><Collection /></el-icon>{{ product.categoryName }}
                    </span>
                </div>

                <div class="stats-row">
                    <div class="stat-item"><el-icon><View /></el-icon><span>{{ product.viewCount ?? 0 }} 次浏览</span></div>
                    <div class="stat-item"><el-icon><Star /></el-icon><span>{{ product.favoriteCount ?? 0 }} 收藏</span></div>
                    <div class="stat-item"><el-icon><ChatDotSquare /></el-icon><span>{{ product.wantCount ?? 0 }} 想要</span></div>
                </div>

                <div class="meta-row">
                    <span class="meta-label">发布时间</span>
                    <span class="meta-value">{{ formatTime(product.createTime) }}</span>
                </div>
            </div>

            <!-- ====== 发布者信息 ====== -->
            <div class="seller-section">
                <div class="seller-card">
                    <el-avatar :size="44" v-if="product.userAvatar" :src="getImageUrl(product.userAvatar)" />
                    <el-avatar :size="44" v-else>{{ (product.username ?? '?')[0] }}</el-avatar>
                    <div class="seller-info">
                        <div class="seller-name-row">
                            <span class="seller-name">{{ product.username || '未知用户' }}</span>
                        </div>
                        <span class="seller-time">发布于 {{ relativeTime(product.createTime) }}</span>
                    </div>
                </div>
            </div>

            <!-- ====== Tab 切换栏 ====== -->
            <div class="tab-bar">
                <div class="tab-panel">
                    <div v-for="tab in tabList" :key="tab.key" :class="['tab-item', { active: activeTab === tab.key }]" @click="activeTab = tab.key">
                        <el-icon :size="16"><component :is="tab.icon" /></el-icon>{{ tab.label }}
                    </div>
                </div>
            </div>

            <!-- ====== 商品图册 ====== -->
            <div class="gallery-section" v-show="activeTab === 'gallery'">
                <div class="thumbnail-row" v-if="imageList.length > 0">
                    <div v-for="(img, index) in imageList" :key="index" class="thumbnail-item" @click="openPreview(index)">
                        <el-image :src="getImageUrl(img.imageUrl)" fit="cover" class="thumbnail-img">
                            <template #error><div class="img-placeholder"><el-icon :size="20"><Picture /></el-icon></div></template>
                        </el-image>
                    </div>
                </div>
                <div class="thumbnail-row empty" v-else>
                    <div class="img-placeholder"><el-icon :size="48"><Picture /></el-icon></div>
                </div>
            </div>

            <!-- 图片预览弹窗 -->
            <el-dialog v-model="dialogVisible" width="60%" top="4vh" :close-on-click-modal="true" destroy-on-close class="image-preview-dialog">
                <div class="preview-container">
                    <el-image :src="dialogImageUrl" fit="contain" class="preview-image">
                        <template #error><div class="preview-placeholder"><el-icon :size="48"><Picture /></el-icon><span>图片加载失败</span></div></template>
                    </el-image>
                </div>
                <div class="preview-thumbnails" v-if="imageList.length > 1">
                    <div v-for="(img, index) in imageList" :key="index" :class="['preview-thumb-item', { active: activeImageIndex === index }]" @click="switchPreviewImage(index)">
                        <el-image :src="getImageUrl(img.imageUrl)" fit="cover" class="preview-thumb-img" />
                    </div>
                </div>
                <div class="preview-nav"><span class="preview-counter">{{ activeImageIndex + 1 }} / {{ imageList.length }}</span></div>
            </el-dialog>

            <!-- ====== 商品描述 ====== -->
            <div class="desc-section" v-show="activeTab === 'description' && product.description">
                <div class="desc-content">{{ product.description }}</div>
            </div>

            <!-- ====== 评价区 ====== -->
            <div class="review-section" v-show="activeTab === 'reviews'">
                <h3 class="section-title">
                    商品评价
                    <span class="review-stats-badge" v-if="reviewStats && reviewStats.reviewCount > 0">
                        <el-rate :model-value="reviewStats.avgRating" disabled :max="5" allow-half show-score score-template="{value}" size="small" />
                        <span class="review-count">（{{ reviewStats.reviewCount }} 条评价）</span>
                    </span>
                </h3>
                <div v-loading="reviewLoading" class="review-list">
                    <el-empty v-if="!reviewLoading && reviewList.length === 0" description="暂无评价" :image-size="60" />
                    <div v-for="review in reviewList" :key="review.id" class="review-item">
                        <div class="review-header">
                            <el-avatar :size="32" v-if="review.buyerAvatar" :src="getImageUrl(review.buyerAvatar)" />
                            <el-avatar :size="32" v-else>{{ (review.buyerName ?? '?')[0] }}</el-avatar>
                            <span class="review-buyer">{{ review.buyerName || '匿名用户' }}</span>
                            <el-rate :model-value="review.rating" disabled :max="5" size="small" class="review-rating" />
                            <span class="review-time">{{ relativeTime(review.createTime) }}</span>
                        </div>
                        <div class="review-content">{{ review.content }}</div>
                        <div class="review-images" v-if="review.images && review.images.length > 0">
                            <el-image v-for="(img, idx) in review.images" :key="idx" class="review-img" :src="getImageUrl(img)" fit="cover" :preview-src-list="(review.images ?? []).map(i => getImageUrl(i))" preview-teleported />
                        </div>
                        <div class="seller-reply" v-if="review.replyContent">
                            <span class="reply-label">卖家回复：</span><span class="reply-text">{{ review.replyContent }}</span>
                            <span class="reply-time" v-if="review.replyTime">{{ relativeTime(review.replyTime) }}</span>
                        </div>
                    </div>
                    <div class="review-pagination" v-if="reviewTotal > pageSize">
                        <el-pagination v-model:current-page="reviewPage" :page-size="pageSize" :total="reviewTotal" layout="prev, pager, next" small background @current-change="loadReviews" />
                    </div>
                </div>
            </div>
        </template>

        <el-empty v-else-if="!loading" description="商品不存在或已下架" />

        <!-- ====== 回到顶部 ====== -->
        <transition name="fade">
            <div v-if="showBackTop" class="back-to-top" @click="scrollToTop"><el-icon :size="22"><ArrowUp /></el-icon></div>
        </transition>
    </div>
</template>

<script lang="ts" setup>
/**
 * 游客端 - 商品详情页
 * <p>展示商品信息、图册、描述、评价。无需登录即可浏览。</p>
 */
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowUp, Picture, View, Star, ChatDotSquare, Medal, Van, Collection, Document } from '@element-plus/icons-vue'
import { getGuestProductDetail, getGuestReviewList, getGuestReviewStats } from '@/api/guest-api'
import type { ProductVO, ProductImageVO, ProductReviewVO } from '@/types/guest'

const route = useRoute()
const router = useRouter()

const product = ref<ProductVO | null>(null)
const loading = ref(false)
const activeImageIndex = ref(0)
const dialogVisible = ref(false)
const dialogImageUrl = ref('')

// 评价
const reviewList = ref<ProductReviewVO[]>([])
const reviewStats = ref<{ avgRating: number; reviewCount: number } | null>(null)
const reviewLoading = ref(false)
const reviewPage = ref(1)
const reviewTotal = ref(0)
const pageSize = 10

// Tab
const activeTab = ref('gallery')
const tabList = [
    { key: 'gallery', label: '商品图册', icon: Picture },
    { key: 'description', label: '商品描述', icon: Document },
    { key: 'reviews', label: '商品评价', icon: ChatDotSquare },
]

const imageList = computed<ProductImageVO[]>(() => product.value?.images ?? [])

// 工具函数
const getImageUrl = (path?: string) => {
    if (!path) return ''; if (path.startsWith('http')) return path
    return `${import.meta.env.VITE_API_BASE_URL}${path}`
}
const conditionLabel = (v?: number) => ({ 1: '全新', 2: '几乎全新', 3: '有使用痕迹', 4: '老旧' } as Record<number,string>)[v ?? 0] || ''
const conditionType = (v?: number) => ({ 1: 'success', 2: 'warning', 3: 'danger', 4: 'info' } as Record<number,string>)[v ?? 0] || ''
const tradeTypeLabel = (v?: number) => ({ 1: '自提', 2: '邮寄', 3: '两者皆可' } as Record<number,string>)[v ?? 0] ?? ''

const formatTime = (t?: string) => {
    if (!t) return ''; const date = new Date(t); const now = new Date()
    const diff = now.getTime() - date.getTime()
    const m = Math.floor(diff / 60000); const h = Math.floor(diff / 3600000); const d = Math.floor(diff / 86400000)
    if (m < 1) return '刚刚'; if (m < 60) return `${m}分钟前`; if (h < 24) return `${h}小时前`; if (d < 7) return `${d}天前`
    return date.toLocaleDateString('zh-CN')
}
const relativeTime = (t?: string) => formatTime(t)

const goBack = () => router.back()

// 图片预览
const openPreview = (index: number) => {
    if (!imageList.value[index]) return
    activeImageIndex.value = index; dialogImageUrl.value = getImageUrl(imageList.value[index].imageUrl); dialogVisible.value = true
}
const switchPreviewImage = (index: number) => {
    if (index < 0 || index >= imageList.value.length) return
    activeImageIndex.value = index; dialogImageUrl.value = getImageUrl(imageList.value[index].imageUrl)
}

// 加载评价
const loadReviews = async () => {
    const id = Number(route.params.id); if (!id) return
    reviewLoading.value = true
    try {
        const res = await getGuestReviewList({ productId: id, current: reviewPage.value, size: pageSize })
        reviewList.value = (res?.data ?? []) as any[]; reviewTotal.value = (res as any)?.count ?? 0
    } catch { /* silent */ } finally { reviewLoading.value = false }
}
const loadReviewStats = async () => {
    const id = Number(route.params.id); if (!id) return
    try { const res = await getGuestReviewStats(id); reviewStats.value = res?.data as any ?? null } catch { /* silent */ }
}

// 加载商品
const loadProductData = async () => {
    const id = Number(route.params.id)
    if (!id || isNaN(id)) { ElMessage.error('商品ID无效'); return }
    loading.value = true; activeTab.value = 'gallery'; reviewPage.value = 1; reviewList.value = []; reviewTotal.value = 0; reviewStats.value = null
    try {
        const res = await getGuestProductDetail(id)
        product.value = res?.data ?? null
        const coverIdx = imageList.value.findIndex(img => img.isCover === 1); if (coverIdx >= 0) activeImageIndex.value = coverIdx
        loadReviews(); loadReviewStats()
    } catch (e: any) { ElMessage.error(e.message || '加载商品详情失败') }
    finally { loading.value = false }
}

watch(() => route.params.id, () => { loadProductData(); window.scrollTo({ top: 0, behavior: 'smooth' }) })

// 返回顶部
const showBackTop = ref(false)
const handleScroll = () => { showBackTop.value = (window.scrollY || document.documentElement.scrollTop) > 400 }
const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

onMounted(() => { loadProductData(); window.addEventListener('scroll', handleScroll) })
onBeforeUnmount(() => { window.removeEventListener('scroll', handleScroll) })
</script>

<style scoped lang="scss">
.product-detail { max-width: 860px; margin: 0 auto; padding-bottom: 40px; }

.back-bar { display: flex; align-items: center; gap: 8px; padding: 16px 0; color: #636366; font-size: 15px; cursor: pointer; user-select: none; transition: color 0.2s; &:hover { color: #e85d3a; } }

.tab-bar { display: flex; justify-content: center; margin-bottom: 16px; .tab-panel { display: inline-flex; background: rgb(240,240,240); border-radius: 30px; padding: 4px; .tab-item { display: inline-flex; align-items: center; gap: 6px; padding: 5px 18px; border-radius: 20px; font-size: 14px; font-weight: 500; color: #666; cursor: pointer; transition: all 0.2s; user-select: none; &.active { background: #fff; color: #000; font-weight: 600; } &:not(.active):hover { color: #333; } } } }

.info-section { background: #fff; border-radius: 14px; padding: 24px; margin-bottom: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.info-header { margin-bottom: 16px; }
.product-title { font-size: 38px; font-weight: 700; color: #1a1a2e; line-height: 1.4; margin-bottom: 16px; }
.price-row { display: flex; align-items: baseline; gap: 12px; padding: 14px 16px; background: linear-gradient(135deg,#fff5f5,#fff0f0); border-radius: 10px; .price-amount { display: flex; align-items: baseline; gap: 2px; .price-symbol { font-size: 18px; font-weight: 700; color: #ff6b35; } .price { font-size: 32px; font-weight: 800; color: #ff6b35; letter-spacing: -1px; } } .original-price { font-size: 14px; color: #999; text-decoration: line-through; } }

.tag-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; .tag-item { display: inline-flex; align-items: center; gap: 5px; padding: 5px 14px; border-radius: 20px; font-size: 13px; font-weight: 500; &.tag-success { background: #e8f5e9; color: #2e7d32; } &.tag-warning { background: #fff8e1; color: #f57c00; } &.tag-danger { background: #fbe9e7; color: #c62828; } &.tag-info { background: #e3f2fd; color: #1565c0; } &.tag-trade { background: #f3e5f5; color: #7b1fa2; } &.tag-category { background: #e0f2f1; color: #00695c; } } }
.stats-row { display: flex; gap: 32px; padding: 16px 0; margin-bottom: 16px; .stat-item { display: flex; align-items: center; gap: 6px; font-size: 15px; color: #636366; .el-icon { font-size: 18px; color: #aeaeb2; } } }
.meta-row { display: flex; justify-content: space-between; padding: 8px 0; font-size: 15px; .meta-label { color: #aeaeb2; } .meta-value { color: #636366; } }

.seller-section { background: #fff; border-radius: 12px; padding: 20px 24px; margin-bottom: 16px; box-shadow: 0 4px 16px rgba(0,0,0,0.06); }
.seller-card { display: flex; align-items: center; gap: 14px; .seller-info { display: flex; flex-direction: column; gap: 4px; flex: 1; .seller-name-row { display: flex; align-items: center; gap: 12px; .seller-name { font-size: 15px; font-weight: 700; color: #1a1a1a; } } .seller-time { font-size: 13px; color: #999; } } }

.gallery-section { background: #fff; border-radius: 14px; margin-bottom: 16px; padding: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); &.empty { display: flex; align-items: center; justify-content: center; min-height: 100px; } }
.thumbnail-row { display: flex; flex-wrap: wrap; gap: 10px; .thumbnail-item { width: 80px; height: 80px; flex-shrink: 0; border-radius: 8px; overflow: hidden; cursor: pointer; border: 2px solid transparent; transition: all 0.25s; background: #f5f5f7; &:hover { border-color: #e85d3a; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(232,93,58,0.15); } .thumbnail-img { width: 100%; height: 100%; } .img-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #bbb; } } }

:deep(.image-preview-dialog) { .el-dialog__body { padding: 0 20px 16px; } }
.preview-container { width: 100%; height: 480px; display: flex; align-items: center; justify-content: center; border-radius: 8px; overflow: hidden; .preview-image { width: 100%; height: 100%; object-fit: contain; } .preview-placeholder { display: flex; flex-direction: column; align-items: center; gap: 8px; color: #bbb; font-size: 13px; } }
.preview-thumbnails { display: flex; justify-content: center; gap: 8px; margin-top: 14px; padding: 10px 0; .preview-thumb-item { width: 52px; height: 52px; flex-shrink: 0; border-radius: 6px; overflow: hidden; cursor: pointer; border: 2px solid transparent; transition: all 0.2s; background: #f0f0f0; &.active { border-color: #e85d3a; } &:hover:not(.active) { border-color: #ccc; } .preview-thumb-img { width: 100%; height: 100%; object-fit: cover; } } }
.preview-nav { display: flex; justify-content: center; margin-top: 10px; .preview-counter { padding: 4px 14px; background: #f0f2f5; border-radius: 20px; font-size: 12px; color: #636366; font-weight: 500; } }

.desc-section { background: #fff; border-radius: 14px; padding: 24px; margin-bottom: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.section-title { font-size: 18px; font-weight: 600; color: #1a1a1a; margin-bottom: 16px; }
.desc-content { font-size: 15px; color: #636366; line-height: 1.8; white-space: pre-wrap; word-break: break-word; }

.review-section { background: #fff; border-radius: 14px; padding: 24px; margin-bottom: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); .review-stats-badge { display: inline-flex; align-items: center; gap: 10px; margin-left: 14px; font-size: 14px; color: #636366; vertical-align: middle; .review-count { color: #aeaeb2; } } }
.review-list { .review-item { padding: 16px 0; border-bottom: 1px solid #f0f0f4; &:last-child { border-bottom: none; } .review-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; .review-buyer { font-size: 15px; font-weight: 500; color: #1a1a1a; } .review-rating { margin-left: 6px; } .review-time { margin-left: auto; font-size: 13px; color: #aeaeb2; } } .review-content { font-size: 14px; color: #636366; line-height: 1.6; margin-bottom: 10px; white-space: pre-wrap; } .review-images { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 10px; .review-img { width: 72px; height: 72px; border-radius: 6px; cursor: pointer; object-fit: cover; border: 1px solid #f0f0f4; &:hover { opacity: 0.85; } } } .seller-reply { background: #f5f7fa; border-radius: 8px; padding: 10px 14px; font-size: 13px; color: #636366; line-height: 1.6; .reply-label { font-weight: 600; color: #1a1a1a; } .reply-time { display: inline-block; margin-left: 8px; font-size: 11px; color: #aeaeb2; } } } .review-pagination { display: flex; justify-content: center; padding-top: 16px; } }

.back-to-top { position: fixed; right: 28px; bottom: 36px; width: 44px; height: 44px; border-radius: 50%; background: #fff; border: 1.5px solid #e0e0e0; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #333; box-shadow: 0 4px 16px rgba(0,0,0,0.1); transition: all 0.25s; z-index: 1000; &:hover { background: #1a1a1a; color: #fff; border-color: #1a1a1a; box-shadow: 0 6px 20px rgba(0,0,0,0.2); transform: translateY(-2px); } }
.fade-enter-active,.fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from,.fade-leave-to { opacity: 0; }
</style>
