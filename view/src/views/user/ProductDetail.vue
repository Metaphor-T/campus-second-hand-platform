<template>
    <div class="product-detail" v-loading="loading">
        <!-- 返回按钮 -->
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

                <!-- 标签行 -->
                <div class="tag-row">
                    <span class="tag-item" :class="'tag-' + conditionType(product.condition)">
                        <el-icon :size="13"><Medal /></el-icon>
                        {{ conditionLabel(product.condition) }}
                    </span>
                    <span class="tag-item tag-trade">
                        <el-icon :size="13"><Van /></el-icon>
                        {{ tradeTypeLabel(product.tradeType) }}
                    </span>
                    <span v-if="product.categoryName" class="tag-item tag-category">
                        <el-icon :size="13"><Collection /></el-icon>
                        {{ product.categoryName }}
                    </span>
                </div>

                <!-- 统计数据 -->
                <div class="stats-row">
                    <div class="stat-item">
                        <el-icon><View /></el-icon>
                        <span>{{ product.viewCount ?? 0 }} 次浏览</span>
                    </div>
                    <div class="stat-item">
                        <el-icon><Star /></el-icon>
                        <span>{{ product.favoriteCount ?? 0 }} 收藏</span>
                    </div>
                    <div class="stat-item">
                        <el-icon><ChatDotSquare /></el-icon>
                        <span>{{ product.wantCount ?? 0 }} 想要</span>
                    </div>
                </div>

                <!-- 库存指标 -->
                <div class="stock-indicator">
                    <template v-if="product.stock != null && product.stock > 0">
                        <div class="stock-inner" :class="{ 'stock-warning': product.stock <= 5 }">
                            <el-icon :size="18" v-if="product.stock <= 5"><WarningFilled /></el-icon>
                            <el-icon :size="18" v-else><CircleCheckFilled /></el-icon>
                            <span class="stock-text">
                                <template v-if="product.stock <= 5">
                                    库存紧张，仅剩 <strong>{{ product.stock }}</strong> 件
                                </template>
                                <template v-else>
                                    库存充足（{{ product.stock }} 件）
                                </template>
                            </span>
                        </div>
                        <el-progress
                            :percentage="stockPercent"
                            :stroke-width="6"
                            :color="product.stock <= 5 ? '#e6a23c' : '#67c23a'"
                            :show-text="false"
                            class="stock-bar"
                        />
                    </template>
                    <div class="stock-inner stock-unlimited" v-else>
                        <el-icon :size="18"><CircleCheckFilled /></el-icon>
                        <span class="stock-text">无限量供应</span>
                    </div>
                </div>

                <!-- 发布信息 -->
                <div class="meta-row">
                    <span class="meta-label">发布时间</span>
                    <span class="meta-value">{{ formatTime(product.createTime) }}</span>
                </div>
                <div class="meta-row" v-if="product.updateTime">
                    <span class="meta-label">更新时间</span>
                    <span class="meta-value">{{ formatTime(product.updateTime) }}</span>
                </div>

                <!-- 操作按钮 -->
                <div class="action-row">
                    <el-button
                        :class="['btn-favorite', { 'is-active': favorited }]"
                        @click="toggleFavorite"
                        :loading="actionLoading"
                        round
                    >
                        <el-icon :size="16" style="margin-right: 4px;">
                            <Star />
                        </el-icon>
                        {{ favorited ? '已收藏' : '收藏' }}
                    </el-button>
                    <el-button
                        type="warning"
                        :disabled="wanted"
                        @click="handleWant"
                        :loading="actionLoading"
                        round
                    >
                        <el-icon :size="16" style="margin-right: 4px;"><ChatDotSquare /></el-icon>
                        {{ wanted ? '已想要' : '我想要' }}
                        <span class="btn-badge">{{ product?.wantCount ?? 0 }}</span>
                    </el-button>
                    <el-button
                        type="primary"
                        @click="goCreateOrder"
                        :disabled="isOwnProduct"
                        round
                        size="default"
                        class="btn-buy"
                    >
                        <el-icon :size="16" style="margin-right: 4px;"><ShoppingCart /></el-icon>
                        {{ isOwnProduct ? '自己的商品' : '立即下单' }}
                    </el-button>
                </div>
            </div>

            <!-- ====== 发布者信息 ====== -->
            <div class="seller-section">
                <div class="seller-card">
                    <el-avatar
                        :size="44"
                        :src="getImageUrl(product.userAvatar)"
                        v-if="product.userAvatar"
                    />
                    <el-avatar :size="44" v-else>{{ (product.username ?? '?')[0] }}</el-avatar>
                    <div class="seller-info">
                        <div class="seller-name-row">
                            <span class="seller-name">{{ product.username || '未知用户' }}</span>
                            <el-button
                                v-if="!isOwnProduct && product.userId"
                                size="small"
                                round
                                @click="contactSeller"
                                class="btn-contact"
                            >
                                <el-icon :size="13" style="margin-right: 2px;"><ChatLineSquare /></el-icon>
                                联系卖家
                            </el-button>
                        </div>
                        <span class="seller-time">发布于 {{ relativeTime(product.createTime) }}</span>
                    </div>
                </div>
            </div>

            <!-- ====== Tab 切换栏 ====== -->
            <div class="tab-bar">
                <div class="tab-panel">
                    <div
                        v-for="tab in tabList"
                        :key="tab.key"
                        :class="['tab-item', { active: activeTab === tab.key }]"
                        @click="activeTab = tab.key"
                    >
                        <el-icon :size="16"><component :is="tab.icon" /></el-icon>
                        {{ tab.label }}
                    </div>
                </div>
            </div>

            <!-- ====== 商品图册 ====== -->
            <div class="gallery-section" v-show="activeTab === 'gallery'">
                <div class="thumbnail-row" v-if="imageList.length > 0">
                    <div
                        v-for="(img, index) in imageList"
                        :key="index"
                        class="thumbnail-item"
                        @click="openPreview(index)"
                    >
                        <el-image
                            :src="getImageUrl(img.imageUrl)"
                            fit="cover"
                            class="thumbnail-img"
                        >
                            <template #error>
                                <div class="img-placeholder">
                                    <el-icon :size="20"><Picture /></el-icon>
                                </div>
                            </template>
                        </el-image>
                    </div>
                </div>
                <div class="thumbnail-row empty" v-else>
                    <div class="img-placeholder">
                        <el-icon :size="48"><Picture /></el-icon>
                    </div>
                </div>
            </div>

            <!-- ====== 图片预览弹窗 ====== -->
            <el-dialog
                v-model="dialogVisible"
                :width="previewDialogWidth"
                top="4vh"
                :close-on-click-modal="true"
                destroy-on-close
                class="image-preview-dialog"
            >
                <div class="preview-container">
                    <el-image
                        :src="dialogImageUrl"
                        fit="contain"
                        class="preview-image"
                    >
                        <template #error>
                            <div class="preview-placeholder">
                                <el-icon :size="48"><Picture /></el-icon>
                                <span>图片加载失败</span>
                            </div>
                        </template>
                    </el-image>
                </div>
                <!-- 弹窗底部缩略图导航 -->
                <div class="preview-thumbnails" v-if="imageList.length > 1">
                    <div
                        v-for="(img, index) in imageList"
                        :key="index"
                        :class="['preview-thumb-item', { active: activeImageIndex === index }]"
                        @click="switchPreviewImage(index)"
                    >
                        <el-image
                            :src="getImageUrl(img.imageUrl)"
                            fit="cover"
                            class="preview-thumb-img"
                        />
                    </div>
                </div>
                <div class="preview-nav">
                    <span class="preview-counter">{{ activeImageIndex + 1 }} / {{ imageList.length }}</span>
                </div>
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
                    <!-- 空评价 -->
                    <el-empty v-if="!reviewLoading && reviewList.length === 0" description="暂无评价" :image-size="60" />

                    <!-- 评价项 -->
                    <div v-for="review in reviewList" :key="review.id" class="review-item">
                        <div class="review-header">
                            <el-avatar :size="32" v-if="review.buyerAvatar" :src="getImageUrl(review.buyerAvatar)" />
                            <el-avatar :size="32" v-else>{{ (review.buyerName ?? '?')[0] }}</el-avatar>
                            <span class="review-buyer">{{ review.buyerName || '匿名用户' }}</span>
                            <el-rate :model-value="review.rating" disabled :max="5" size="small" class="review-rating" />
                            <span class="review-time">{{ relativeTime(review.createTime) }}</span>
                        </div>
                        <div class="review-content">{{ review.content }}</div>
                        <!-- 评价图片 -->
                        <div class="review-images" v-if="review.images && review.images.length > 0">
                            <el-image
                                v-for="(img, idx) in review.images"
                                :key="idx"
                                class="review-img"
                                :src="getImageUrl(img)"
                                fit="cover"
                                :preview-src-list="(review.images ?? []).map(i => getImageUrl(i))"
                                preview-teleported
                            />
                        </div>
                        <!-- 卖家回复 -->
                        <div class="seller-reply" v-if="review.replyContent">
                            <span class="reply-label">卖家回复：</span>
                            <span class="reply-text">{{ review.replyContent }}</span>
                            <span class="reply-time" v-if="review.replyTime">{{ relativeTime(review.replyTime) }}</span>
                        </div>
                    </div>

                    <!-- 分页 -->
                    <div class="review-pagination" v-if="reviewTotal > pageSize">
                        <el-pagination
                            v-model:current-page="reviewPage"
                            :page-size="pageSize"
                            :total="reviewTotal"
                            layout="prev, pager, next"
                            small
                            background
                            @current-change="loadReviews"
                        />
                    </div>
                </div>
            </div>

            <!-- ====== 发布者在售 ====== -->
            <div class="seller-products-section" v-if="sellerProducts.length > 0">
                <h3 class="seller-products-title">该发布者其他在售商品</h3>
                <div class="seller-products-grid">
                    <div
                        v-for="item in sellerProducts"
                        :key="item.id"
                        class="seller-product-item"
                        @click="goProduct(item)"
                    >
                        <div class="product-card">
                            <div class="card-image">
                                <img :src="getImageUrl(coverImage(item))" class="card-img" loading="lazy" />
                                <span class="condition-tag" :class="'cond-' + (item.condition ?? 1)">
                                    {{ conditionLabel(item.condition) }}
                                </span>
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
                                    <span class="stat-item"><el-icon><ShoppingCart /></el-icon>{{ item.wantCount ?? 0 }}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </template>

        <!-- 商品不存在提示 -->
        <el-empty v-else-if="!loading" description="商品不存在或已下架" />

        <!-- ====== 返回顶部 ====== -->
        <transition name="fade">
            <div v-if="showBackTop" class="back-to-top" @click="scrollToTop">
                <el-icon :size="22"><ArrowUp /></el-icon>
            </div>
        </transition>
    </div>
</template>

<script lang="ts" setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowUp, Picture, View, Star, ChatDotSquare, ChatLineSquare, ShoppingCart, WarningFilled, CircleCheckFilled, Medal, Van, Collection, Document } from '@element-plus/icons-vue'
import { userTokenAuthApi } from '@/api/user-api'
import { getProductDetailApi, toggleFavoriteApi, addWantApi, getInteractStatusApi, recordViewApi, publicProductListApi } from '@/api/product-api'
import { publicReviewListApi, getReviewStatsApi } from '@/api/review-api'
import type { ProductVO, ProductImageVO } from '@/types/product'
import type { ProductReviewVO } from '@/types/review'

const route = useRoute()
const router = useRouter()

const product = ref<ProductVO | null>(null)
const loading = ref(false)
const activeImageIndex = ref(0)
const favorited = ref(false)
const wanted = ref(false)
const actionLoading = ref(false)
const currentUserId = ref<number | undefined>(undefined)

// ====== 评价相关 ======
const reviewList = ref<ProductReviewVO[]>([])
const reviewStats = ref<{ avgRating: number; reviewCount: number } | null>(null)
const reviewLoading = ref(false)
const reviewPage = ref(1)
const reviewTotal = ref(0)
const pageSize = 10

// 图片列表
const imageList = computed<ProductImageVO[]>(() => {
    return product.value?.images ?? []
})

// ====== 发布者在售商品 ======
const sellerProducts = ref<ProductVO[]>([])
const sellerLoading = ref(false)

/** 获取封面图URL */
const coverImage = (item: ProductVO) => {
    const cover = item.images?.find(i => i.isCover === 1)
    return cover?.imageUrl ?? item.images?.[0]?.imageUrl
}

/** 交易方式标签 */
const tradeLabel = (v?: number) => {
    const map: Record<number, string> = { 1: '自提', 2: '邮寄', 3: '皆可' }
    return v ? map[v] ?? '' : ''
}

/** 跳转到指定商品详情 */
const goProduct = (item: ProductVO) => {
    if (item.id) {
        router.push(`/user-home/product-detail/${item.id}`)
    }
}

// ====== 返回顶部 ======
const showBackTop = ref(false)

const handleScroll = () => {
    const scrollTop = window.scrollY || document.documentElement.scrollTop || 0
    showBackTop.value = scrollTop > 400
}

const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 图片预览弹窗
const dialogVisible = ref(false)
const dialogImageUrl = ref('')

// 预览弹窗宽度（响应式适配）
const previewDialogWidth = computed(() => {
    return window.innerWidth < 640 ? '95%' : '620px'
})

// ====== Tab 切换 ======
const activeTab = ref('gallery')

interface TabItem {
    key: string
    label: string
    icon: any
}

const tabList: TabItem[] = [
    { key: 'gallery', label: '商品图册', icon: Picture },
    { key: 'description', label: '商品描述', icon: Document },
    { key: 'reviews', label: '商品评价', icon: ChatDotSquare },
]

// ====== 工具函数 ======
const getImageUrl = (path?: string) => {
    if (!path) return ''
    if (path.startsWith('http://') || path.startsWith('https://')) return path
    return `${import.meta.env.VITE_API_BASE_URL}${path}`
}

const conditionLabel = (v?: number) => {
    const map: Record<number, string> = { 1: '全新', 2: '几乎全新', 3: '有使用痕迹', 4: '老旧' }
    return v ? map[v] || '' : ''
}

const conditionType = (v?: number) => {
    const map: Record<number, string> = { 1: 'success', 2: 'warning', 3: 'danger', 4: 'info' }
    return v ? map[v] || '' : ''
}

const tradeTypeLabel = (v?: number) => {
    const map: Record<number, string> = { 1: '自提', 2: '邮寄', 3: '两者皆可' }
    return v ? map[v] || '' : ''
}

const formatTime = (t?: string) => {
    if (!t) return ''
    const date = new Date(t)
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(diff / 3600000)
    const days = Math.floor(diff / 86400000)
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    if (hours < 24) return `${hours}小时前`
    if (days < 7) return `${days}天前`
    return date.toLocaleDateString('zh-CN')
}

const relativeTime = (t?: string) => formatTime(t)

// 判断是否为自己的商品
const isOwnProduct = computed(() => {
    return currentUserId.value && product.value?.userId === currentUserId.value
})

// 库存百分比（用于进度条，上限100%）
const stockPercent = computed(() => {
    const s = product.value?.stock
    if (s == null || s <= 0) return 0
    return Math.min(Math.round((s / 100) * 100), 100)
})

// ====== 图片交互（缩略图行 + 弹窗） ======
const openPreview = (index: number) => {
    if (!imageList.value[index]) return
    activeImageIndex.value = index
    dialogImageUrl.value = getImageUrl(imageList.value[index].imageUrl)
    dialogVisible.value = true
}

const switchPreviewImage = (index: number) => {
    if (index < 0 || index >= imageList.value.length) return
    activeImageIndex.value = index
    dialogImageUrl.value = getImageUrl(imageList.value[index].imageUrl)
}

const goBack = () => {
    router.back()
}

// ====== 跳转下单 ======
const goCreateOrder = () => {
    const id = Number(route.params.id)
    if (!id || isOwnProduct.value) return
    router.push(`/user-home/create-order/${id}`)
}

// ====== 联系卖家 ======
const contactSeller = () => {
    if (!product.value?.userId) return
    router.push(`/user-home/chat/${product.value.userId}`)
}

// ====== 收藏 / 想要 ======
const toggleFavorite = async () => {
    const id = Number(route.params.id)
    if (!id) return
    actionLoading.value = true
    try {
        const res = await toggleFavoriteApi(id)
        favorited.value = res?.data?.favorited ?? !favorited.value
        // 更新本地的收藏数
        if (product.value) {
            product.value.favoriteCount = (product.value.favoriteCount ?? 0) + (favorited.value ? 1 : -1)
            if (product.value.favoriteCount < 0) product.value.favoriteCount = 0
        }
    } catch (e: any) {
        ElMessage.error(e.message || '操作失败')
    } finally {
        actionLoading.value = false
    }
}

const handleWant = async () => {
    if (wanted.value) return
    const id = Number(route.params.id)
    if (!id) return
    actionLoading.value = true
    try {
        await addWantApi(id)
        wanted.value = true
        // 更新本地的想要数
        if (product.value) {
            product.value.wantCount = (product.value.wantCount ?? 0) + 1
        }
        ElMessage.success('已标记想要')
    } catch (e: any) {
        ElMessage.error(e.message || '操作失败')
    } finally {
        actionLoading.value = false
    }
}

// ====== 加载评价 ======
const loadReviews = async () => {
    const id = Number(route.params.id)
    if (!id) return
    reviewLoading.value = true
    try {
        const res = await publicReviewListApi({ productId: id, current: reviewPage.value, size: pageSize })
        reviewList.value = res?.data ?? []
        reviewTotal.value = res?.count ?? 0
    } catch {
        // 评价加载失败不影响主体
    } finally {
        reviewLoading.value = false
    }
}

const loadReviewStats = async () => {
    const id = Number(route.params.id)
    if (!id) return
    try {
        const res = await getReviewStatsApi(id)
        reviewStats.value = res?.data ?? null
    } catch {
        // 静默失败
    }
}

// ====== 加载发布者在售商品 ======
const loadSellerProducts = async () => {
    const userId = product.value?.userId
    if (!userId) return
    sellerLoading.value = true
    try {
        const res = await publicProductListApi({ userId, current: 1, size: 8 })
        // 排除当前商品
        const currentId = Number(route.params.id)
        sellerProducts.value = (res?.data ?? []).filter((p: ProductVO) => p.id !== currentId)
    } catch {
        sellerProducts.value = []
    } finally {
        sellerLoading.value = false
    }
}

// ====== 加载数据 ======
const loadProductData = async () => {
    const id = Number(route.params.id)
    if (!id || isNaN(id)) {
        ElMessage.error('商品ID无效')
        return
    }
    loading.value = true
    // 每次切换商品时重置状态
    favorited.value = false
    wanted.value = false
    activeTab.value = 'gallery'
    reviewPage.value = 1
    reviewList.value = []
    reviewTotal.value = 0
    reviewStats.value = null
    sellerProducts.value = []
    try {
        const res = await getProductDetailApi(id)
        product.value = res?.data ?? null
        // 找到封面图的索引
        const coverIndex = imageList.value.findIndex(img => img.isCover === 1)
        if (coverIndex >= 0) {
            activeImageIndex.value = coverIndex
        }

        // 加载交互状态
        try {
            const statusRes = await getInteractStatusApi(id)
            if (statusRes?.data) {
                favorited.value = statusRes.data.favorited
                wanted.value = statusRes.data.wanted
            }
        } catch {
            // 交互状态接口失败不影响主体显示
        }

        // 获取当前用户信息（直接调用API，避免localStorage缓存污染导致误判自营）
        try {
            const authResp = await userTokenAuthApi()
            const authData = authResp?.data as any
            if (authData?.id) {
                currentUserId.value = authData.id
            }
        } catch {
            // 忽略
        }

        // 加载评价
        loadReviews()
        loadReviewStats()

        // 加载发布者在售商品
        loadSellerProducts()

        // 记录浏览（不阻塞主流程）
        recordViewApi(id).then(() => {
            if (product.value) {
                product.value.viewCount = (product.value.viewCount ?? 0) + 1
            }
        }).catch(() => {
            // 浏览记录失败不影响主体显示
        })
    } catch (e: any) {
        ElMessage.error(e.message || '加载商品详情失败')
    } finally {
        loading.value = false
    }
}

// 监听路由参数变化，仅在当前仍为商品详情页且 id 有效时才重新加载
watch(() => route.params.id, (newId) => {
    if (!newId) return
    loadProductData()
    window.scrollTo({ top: 0, behavior: 'smooth' })
})

onMounted(() => {
    loadProductData()
    // 返回顶部滚动监听
    window.addEventListener('scroll', handleScroll)
})

onBeforeUnmount(() => {
    window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;
.product-detail {
    max-width: 860px;
    margin: 0 auto;
    padding-bottom: $spacing-xl;
}

// ====== 返回栏 ======
.back-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: $spacing-lg 0;
    color: $color-text-secondary;
    font-size: $font-size-md;
    cursor: pointer;
    user-select: none;
    transition: color 0.2s;

    &:hover {
        color: $color-primary;
    }
}

// ====== Tab 切换栏（登录页角色切换胶囊全样式） ======
.tab-bar {
    display: flex;
    justify-content: center;
    margin-bottom: $spacing-lg;

    .tab-panel {
        display: inline-flex;
        background-color: rgb(240, 240, 240);
        border-radius: 30px;
        padding: 4px;
        align-items: center;

        .tab-item {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 5px 18px;
            border-radius: 20px;
            font-size: 14px;
            font-weight: 500;
            color: #666;
            cursor: pointer;
            transition: all 0.2s;
            user-select: none;

            .el-icon {
                font-size: 16px;
            }

            &.active {
                background: #fff;
                color: #000;
                font-weight: 600;
            }

            &:not(.active):hover {
                color: #333;
            }
        }
    }
}

// ====== 图册展示区（横向缩略图行） ======
.gallery-section {
    background: $color-surface;
    border-radius: $radius-lg;
    margin-bottom: $spacing-md;
    padding: $spacing-md;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

    &.empty {
        display: flex;
        align-items: center;
        justify-content: center;
        min-height: 100px;
    }
}

.thumbnail-row {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;

    .thumbnail-item {
        width: 80px;
        height: 80px;
        flex-shrink: 0;
        border-radius: 8px;
        overflow: hidden;
        cursor: pointer;
        border: 2px solid transparent;
        transition: all 0.25s ease;
        background: #f5f5f7;

        &:hover {
            border-color: $color-primary;
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba($color-primary, 0.15);
        }

        &:active {
            transform: translateY(0);
        }

        .thumbnail-img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        .img-placeholder {
            width: 100%;
            height: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #bbb;
        }
    }
}

// ====== 图片预览弹窗 ======
:deep(.image-preview-dialog) {
    .el-dialog__body {
        padding: 0 20px 16px;
    }

    .el-dialog__header {
        padding: 16px 20px 0;
    }

    .el-dialog__headerbtn {
        top: 16px;
        right: 20px;
    }
}

.preview-container {
    width: 100%;
    height: 480px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    overflow: hidden;

    .preview-image {
        width: 100%;
        height: 100%;
        object-fit: contain;
        transition: opacity 0.3s;
    }

    .preview-placeholder {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 8px;
        color: #bbb;
        font-size: 13px;
    }
}

.preview-thumbnails {
    display: flex;
    justify-content: center;
    gap: 8px;
    margin-top: 14px;
    padding: 10px 0;

    .preview-thumb-item {
        width: 52px;
        height: 52px;
        flex-shrink: 0;
        border-radius: 6px;
        overflow: hidden;
        cursor: pointer;
        border: 2px solid transparent;
        transition: all 0.2s;
        background: #f0f0f0;

        &.active {
            border-color: $color-primary;
            box-shadow: 0 0 0 1px rgba($color-primary, 0.2);
        }

        &:hover:not(.active) {
            border-color: #ccc;
        }

        .preview-thumb-img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }
    }
}

.preview-nav {
    display: flex;
    justify-content: center;
    margin-top: 10px;

    .preview-counter {
        display: inline-flex;
        align-items: center;
        padding: 4px 14px;
        background: #f0f2f5;
        border-radius: 20px;
        font-size: 12px;
        color: $color-text-secondary;
        font-weight: 500;
        letter-spacing: 0.3px;
    }
}

// ====== 商品信息区 ======
.info-section {
    background: $color-surface;
    border-radius: $radius-lg;
    padding: $spacing-lg;
    margin-bottom: $spacing-md;
    box-shadow: $shadow-sm;
}

.info-header {
    margin-bottom: $spacing-md;
    position: relative;
}

.product-title {
    font-size: 38px;
    font-weight: 700;
    color: #1a1a2e;
    line-height: 1.4;
    margin-bottom: 16px;
}

.price-row {
    display: flex;
    align-items: baseline;
    gap: 12px;
    padding: 14px 16px;
    background: linear-gradient(135deg, #fff5f5, #fff0f0);
    border-radius: 10px;

    .price-amount {
        display: flex;
        align-items: baseline;
        gap: 2px;

        .price-symbol {
            font-size: 18px;
            font-weight: 700;
            color: $color-accent;
        }

        .price {
            font-size: 32px;
            font-weight: 800;
            color: $color-accent;
            letter-spacing: -1px;
        }
    }

    .original-price {
        font-size: 14px;
        color: #999;
        text-decoration: line-through;
    }
}

// ====== 标签行 ======
.tag-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: $spacing-md;

    .tag-item {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        padding: 5px 14px;
        border-radius: 20px;
        font-size: 13px;
        font-weight: 500;
        transition: all 0.2s;

        .el-icon {
            font-size: 14px;
        }

        &.tag-success {
            background: #e8f5e9;
            color: #2e7d32;
        }

        &.tag-warning {
            background: #fff8e1;
            color: #f57c00;
        }

        &.tag-danger {
            background: #fbe9e7;
            color: #c62828;
        }

        &.tag-info {
            background: #e3f2fd;
            color: #1565c0;
        }

        &.tag-trade {
            background: #f3e5f5;
            color: #7b1fa2;
        }

        &.tag-category {
            background: #e0f2f1;
            color: #00695c;
        }
    }
}

// ====== 统计数据 ======
.stats-row {
    display: flex;
    gap: $spacing-xl;
    padding: $spacing-lg 0;
    margin-bottom: $spacing-lg;
}

.stat-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: $font-size-base;
    color: $color-text-secondary;

    .el-icon {
        font-size: 18px;
        color: $color-text-light;
    }
}

// ====== 操作按钮行 ======
.action-row {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-top: $spacing-xl;
    padding-top: $spacing-lg;

    .el-button {
        height: 40px;
        font-size: 15px;
        font-weight: 600;
        border-radius: 20px;
        padding: 0 24px;
        transition: all 0.25s;
    }

    // 收藏按钮 — 白底灰边框，激活转为红底
    .btn-favorite {
        background: #fff;
        border: 1.5px solid #d0d0d0;
        color: #333;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

        &:hover {
            border-color: #ff6b6b;
            color: #ff6b6b;
            box-shadow: 0 2px 12px rgba(255, 107, 107, 0.12);
        }

        &.is-active {
            background: linear-gradient(135deg, #ff6b6b, #ee5a24);
            border-color: transparent;
            color: #fff;
            box-shadow: 0 4px 14px rgba(238, 90, 36, 0.3);

            &:hover {
                background: linear-gradient(135deg, #ff7b7b, #f26a34);
                box-shadow: 0 6px 18px rgba(238, 90, 36, 0.4);
                transform: translateY(-1px);
            }
        }
    }

    // 我想要按钮 — 暖色渐变
    .el-button--warning {
        background: linear-gradient(135deg, #fbb23e, #f59e0b);
        border-color: transparent;
        color: #fff;
        box-shadow: 0 4px 14px rgba(245, 158, 11, 0.3);

        &:hover {
            background: linear-gradient(135deg, #fcc45e, #f6ad1a);
            border-color: transparent;
            color: #fff;
            box-shadow: 0 6px 18px rgba(245, 158, 11, 0.4);
            transform: translateY(-1px);
        }

        &:disabled {
            background: linear-gradient(135deg, #fde68a, #fcd34d);
            box-shadow: none;
            opacity: 0.6;
            cursor: not-allowed;
            transform: none;
        }
    }

    .btn-badge {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 22px;
        height: 22px;
        padding: 0 7px;
        margin-left: 5px;
        border-radius: 11px;
        background: rgba(255, 255, 255, 0.3);
        font-size: 12px;
        font-weight: 700;
        line-height: 1;
    }

    // 立即下单按钮 — 纯黑带阴影（与联系卖家统一风格）
    .btn-buy {
        background: #1a1a1a;
        color: #fff;
        border: none;
        box-shadow: 0 4px 14px rgba(0, 0, 0, 0.2);

        &:hover {
            background: #000;
            color: #fff;
            box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
            transform: translateY(-1px);
        }

        &:active {
            transform: translateY(0);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
        }

        &:disabled {
            background: #888;
            color: #fff;
            border: none;
            box-shadow: none;
            opacity: 0.5;
            cursor: not-allowed;
            transform: none;
        }
    }
}

// ====== 元数据行 ======
.meta-row {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    font-size: $font-size-base;

    .meta-label {
        color: $color-text-light;
    }

    .meta-value {
        color: $color-text-secondary;
    }
}

// ====== 库存指标 ======
.stock-indicator {
    margin: 20px 0;
    padding: 16px 18px;
    background: #fef7e6;
    border: 1px solid #fce4b3;
    border-radius: 12px;
    transition: all 0.2s;

    .stock-inner {
        display: flex;
        align-items: center;
        gap: 10px;
        line-height: 1;

        .el-icon {
            flex-shrink: 0;
            display: inline-flex;
            align-items: center;
            vertical-align: middle;
        }

        &.stock-warning {
            color: #e6a23c;
        }

        &.stock-unlimited {
            color: #67c23a;
        }

        .stock-text {
            font-size: 15px;
            line-height: 1.4;

            strong {
                font-size: 20px;
                font-weight: 700;
            }
        }
    }

    .stock-bar {
        margin-top: 10px;
    }
}

// ====== 发布者信息 ======
.seller-section {
    background: #fff;
    border-radius: 12px;
    padding: 20px 24px;
    margin-bottom: $spacing-lg;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.seller-card {
    display: flex;
    align-items: center;
    gap: 14px;

    .el-avatar {
        border: 2px solid #f0f0f0;
        flex-shrink: 0;
    }

    .seller-info {
        display: flex;
        flex-direction: column;
        gap: 4px;
        flex: 1;

        .seller-name-row {
            display: flex;
            align-items: center;
            gap: 12px;

            .seller-name {
                font-size: 15px;
                font-weight: 700;
                color: #1a1a1a;
            }
        }

        .seller-time {
            font-size: 13px;
            color: #999;
        }
    }

    .btn-contact {
        border: 1.5px solid #d0d0d0;
        color: #333;
        background: #fff;
        font-weight: 600;
        height: 32px;
        font-size: 13px;
        padding: 0 16px;
        transition: all 0.25s;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

        &:hover {
            border-color: #ff6b6b;
            color: #ff6b6b;
            box-shadow: 0 2px 12px rgba(255, 107, 107, 0.12);
        }
    }
}

// ====== 商品描述 ======
.desc-section {
    background: $color-surface;
    border-radius: $radius-lg;
    padding: $spacing-lg;
    margin-bottom: $spacing-md;
    box-shadow: $shadow-sm;
}

.section-title {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $color-text;
    margin-bottom: $spacing-lg;
}

.desc-content {
    font-size: $font-size-md;
    color: $color-text-secondary;
    line-height: 1.8;
    white-space: pre-wrap;
    word-break: break-word;
}

// ====== 评价区 ======
.review-section {
    background: $color-surface;
    border-radius: $radius-lg;
    padding: $spacing-lg;
    margin-bottom: $spacing-md;
    box-shadow: $shadow-sm;

    .review-stats-badge {
        display: inline-flex;
        align-items: center;
        gap: 10px;
        margin-left: 14px;
        font-size: 14px;
        font-weight: 400;
        color: $color-text-secondary;
        vertical-align: middle;

        .review-count {
            color: $color-text-light;
        }
    }
}

.review-list {
    .review-item {
        padding: 16px 0;

        .review-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 10px;

        .review-buyer {
            font-size: 15px;
            font-weight: 500;
            color: $color-text;
        }

        .review-rating {
            margin-left: 6px;
        }

        .review-time {
            margin-left: auto;
            font-size: 13px;
            color: $color-text-light;
        }
        }

        .review-content {
            font-size: 14px;
            color: $color-text-secondary;
            line-height: 1.6;
            margin-bottom: 10px;
            white-space: pre-wrap;
            word-break: break-word;
        }

        .review-images {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-bottom: 10px;

            .review-img {
                width: 72px;
                height: 72px;
                border-radius: 6px;
                cursor: pointer;
                object-fit: cover;
                border: 1px solid $color-border-light;
                transition: opacity 0.2s;

                &:hover {
                    opacity: 0.85;
                }
            }
        }

        .seller-reply {
            background: #f5f7fa;
            border-radius: 8px;
            padding: 10px 14px;
            font-size: 13px;
            color: $color-text-secondary;
            line-height: 1.6;

            .reply-label {
                font-weight: 600;
                color: $color-text;
            }

            .reply-time {
                display: inline-block;
                margin-left: 8px;
                font-size: 11px;
                color: $color-text-light;
            }
        }
    }

    .review-pagination {
        display: flex;
        justify-content: center;
        padding-top: 16px;
    }

}

// =====================================================
// 📦 发布者在售商品
// =====================================================
.seller-products-section {
    margin-bottom: $spacing-md;
}

.seller-products-title {
    font-size: 16px;
    font-weight: 700;
    color: #1a1a2e;
    margin-bottom: 14px;
    padding-bottom: 10px;
    border-bottom: 1px solid #f0f0f4;
}

.seller-products-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;

    @media (max-width: 640px) {
        grid-template-columns: repeat(2, 1fr);
    }
}

.seller-product-item {
    cursor: pointer;
}

// 商品卡片（与首页市场统一风格）
.seller-products-section {
    .product-card {
        background: #fff;
        border-radius: 12px;
        overflow: hidden;
        box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
        cursor: pointer;
        transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.4s ease;

        &:hover {
            transform: translateY(-6px) scale(1.02);
            box-shadow: 0 12px 32px rgba(232, 93, 58, 0.16);
        }

        .card-image {
            position: relative;
            width: 100%;
            overflow: hidden;
            background: #f2f2f7;

            .card-img {
                display: block;
                width: 100%;
                height: auto;
                min-height: 80px;
                transition: transform 0.5s ease;

                &[src=''] { display: none; }
            }

            .product-card:hover & .card-img {
                transform: scale(1.06);
            }

            .condition-tag {
                position: absolute;
                top: 6px;
                left: 6px;
                padding: 2px 10px;
                font-size: 10px;
                color: #fff;
                border-radius: 999px;
                font-weight: 600;
                letter-spacing: 0.3px;

                &.cond-1 { background: rgba(103, 194, 58, 0.8); }
                &.cond-2 { background: rgba(64, 158, 255, 0.8); }
                &.cond-3 { background: rgba(230, 162, 60, 0.8); }
                &.cond-4 { background: rgba(144, 147, 153, 0.8); }
            }

            .trade-badge {
                position: absolute;
                top: 6px;
                right: 6px;
                padding: 2px 8px;
                font-size: 10px;
                color: #fff;
                background: rgba($color-accent, 0.8);
                border-radius: 999px;
                font-weight: 600;
            }
        }

        .card-body {
            padding: 8px 10px 12px;

            .card-title {
                margin: 0 0 6px;
                font-size: 13px;
                font-weight: 500;
                color: #1d1d1f;
                line-height: 1.4;
                display: -webkit-box;
                -webkit-line-clamp: 2;
                -webkit-box-orient: vertical;
                overflow: hidden;
            }

            .card-price {
                display: flex;
                align-items: baseline;
                gap: 4px;
                margin-bottom: 6px;

                .price-symbol {
                    font-size: 11px;
                    font-weight: 600;
                    color: $color-accent;
                }

                .price {
                    font-size: 16px;
                    font-weight: 700;
                    color: $color-accent;
                    letter-spacing: -0.3px;
                }

                .original-price {
                    font-size: 10px;
                    color: #aeaeb2;
                    text-decoration: line-through;
                }
            }

            .card-stats {
                display: flex;
                gap: 8px;

                .stat-item {
                    display: inline-flex;
                    align-items: center;
                    gap: 2px;
                    font-size: 10px;
                    color: #aeaeb2;

                    .el-icon { font-size: 11px; }
                }
            }
        }
    }
}

// =====================================================
// 🔝 返回顶部
// =====================================================
.back-to-top {
    position: fixed;
    right: 28px;
    bottom: 36px;
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: #fff;
    border: 1.5px solid #e0e0e0;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #333;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    transition: all 0.25s;
    z-index: 1000;

    &:hover {
        background: #1a1a1a;
        color: #fff;
        border-color: #1a1a1a;
        box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
        transform: translateY(-2px);
    }

    &:active {
        transform: translateY(0);
    }
}

// fade 过渡
.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}
</style>
