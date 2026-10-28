<template>
    <div class="user-reviews">
        <!-- 搜索栏 -->
        <div class="search-bar">
            <div class="search-wrapper">
                <el-icon class="search-icon"><Search /></el-icon>
                <el-input
                    v-model="keyword"
                    placeholder="搜索评价内容"
                    clearable
                    @clear="doSearch"
                    @keyup.enter="doSearch"
                    class="search-input"
                />
                <el-button class="search-btn" @click="doSearch">搜索</el-button>
            </div>
        </div>

        <div v-loading="loading" class="review-list">
            <el-empty v-if="!loading && reviewList.length === 0" description="暂无评价" />

            <div v-for="review in reviewList" :key="review.id" class="review-card">
                <div class="review-top">
                    <el-image
                        class="product-img"
                        :src="getImageUrl(review.productImage)"
                        fit="cover"
                    >
                        <template #error>
                            <div class="img-placeholder">
                                <el-icon :size="20"><Picture /></el-icon>
                            </div>
                        </template>
                    </el-image>
                    <div class="review-info">
                        <div class="product-title">{{ review.productTitle }}</div>
                        <div class="review-meta">
                            <el-rate :model-value="review.rating" disabled :max="5" size="small" />
                            <span class="review-time">{{ formatTime(review.createTime) }}</span>
                        </div>
                    </div>
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
                </div>
                <!-- 状态标签 -->
                <div class="review-footer">
                    <el-tag v-if="review.status === 0" size="small" type="info">已隐藏</el-tag>
                    <el-tag v-else size="small" type="success">已显示</el-tag>
                </div>
            </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-bar" v-if="totalCount > 0">
            <el-pagination
                v-model:current-page="currentPage"
                :page-size="pageSize"
                :total="totalCount"
                layout="prev, pager, next"
                @current-change="loadReviews"
                size="large"
            />
        </div>
    </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Picture } from '@element-plus/icons-vue'
import { myReviewListApi } from '@/api/review-api'
import type { ProductReviewVO } from '@/types/review'

const loading = ref(false)
const reviewList = ref<ProductReviewVO[]>([])
const totalCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(5)
const keyword = ref('')

const getImageUrl = (path?: string) => {
    if (!path) return ''
    if (path.startsWith('http://') || path.startsWith('https://')) return path
    return `${import.meta.env.VITE_API_BASE_URL}${path}`
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

const doSearch = () => {
    currentPage.value = 1
    loadReviews()
}

const loadReviews = async () => {
    loading.value = true
    try {
        const res = await myReviewListApi({ current: currentPage.value, size: pageSize.value, keyword: keyword.value || undefined })
        reviewList.value = res?.data ?? []
        totalCount.value = res?.count ?? 0
    } catch (e: any) {
        ElMessage.error(e.message || '加载评价失败')
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    loadReviews()
})
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;
.user-reviews {
    margin: 0 auto;
    padding-bottom: 80px;
}

// ====== 搜索栏 ======
.search-bar {
    display: flex;
    gap: 10px;
    margin: 16px 20px;

    .search-wrapper {
        flex: 1;
        max-width: 400px;
        display: flex;
        align-items: center;
        background: #fff;
        border-radius: 22px;
        padding: 6px 6px 6px 16px;
        border: 1px solid #e5e5ea;
        transition: all 0.3s ease;

        &:focus-within {
            border-color: #1a1a1a;
            box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
        }

        .search-icon {
            color: #aeaeb2;
            font-size: 18px;
            flex-shrink: 0;
        }

        .search-input {
            flex: 1;

            :deep(.el-input__wrapper) {
                background: transparent !important;
                box-shadow: none !important;
                padding: 0 10px;
            }

            :deep(.el-input__inner) {
                font-size: 14px;
                border: none;
                background: transparent;
                color: #1a1a1a;

                &::placeholder {
                    color: #aeaeb2;
                }
            }
        }

        .search-btn {
            flex-shrink: 0;
            height: 32px;
            padding: 0 18px;
            border-radius: 16px;
            font-size: 13px;
            background: #1a1a1a;
            color: #fff;
            border: none;
            cursor: pointer;
            transition: all 0.25s ease;
            font-weight: 600;

            &:hover {
                background: #333;
                transform: scale(1.04);
            }
        }
    }
}

// ====== 评价列表 ======
.review-list {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    padding: 0 20px;
}

// ====== 评价卡片 ======
.review-card {
    background: $color-surface;
    border-radius: 10px;
    padding: $spacing-lg;

    .review-top {
        display: flex;
        gap: $spacing-md;
        margin-bottom: 12px;

        .product-img {
            width: 64px;
            height: 64px;
            border-radius: $radius-md;
            flex-shrink: 0;
            background: #f5f5f7;
        }

        .img-placeholder {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 100%;
            height: 100%;
            color: #bbb;
            background: #f5f5f7;
            border-radius: $radius-md;
        }

        .review-info {
            flex: 1;
            display: flex;
            flex-direction: column;
            gap: 6px;
            min-width: 0;

            .product-title {
                font-size: $font-size-base;
                font-weight: 600;
                color: $color-text;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }

            .review-meta {
                display: flex;
                align-items: center;
                gap: 10px;

                .review-time {
                    font-size: 12px;
                    color: $color-text-light;
                }
            }
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
            width: 40px;
            height: 40px;
            border-radius: 6px;
            cursor: pointer;
            object-fit: cover;
            border: 1px solid $color-border-light;
        }
    }

    .seller-reply {
        background: rgb(246, 246, 246);
        border-radius: 8px;
        padding: 10px 14px;
        font-size: 13px;
        color: $color-text-secondary;
        line-height: 1.6;
        margin-bottom: 10px;

        .reply-label {
            font-weight: 600;
            color: $color-text;
        }
    }

    .review-footer {
        display: flex;
        justify-content: flex-end;
    }
}

// ====== 分页 ======
.pagination-bar {
    display: flex;
    justify-content: center;
    padding: 28px 0 20px;

    :deep(.el-pagination) {
        button, .el-pager li {
            font-size: 18px;
            min-width: 36px;
            height: 36px;
            line-height: 36px;
            color: #000 !important;
        }

        .el-pager li.active {
            font-size: 22px;
            font-weight: 700;
            color: #000 !important;
            background: transparent !important;
        }

        .el-pager li:not(.active):hover {
            color: #000 !important;
        }
    }
}
</style>
