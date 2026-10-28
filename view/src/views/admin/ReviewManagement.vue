<template>
    <div class="review-management">
        <!-- 搜索筛选 -->
        <div class="search-bar">
            <el-input
                v-model="keyword"
                placeholder="搜索商品标题"
                clearable
                :prefix-icon="Search"
                @clear="doSearch"
                @keyup.enter="doSearch"
                class="search-input"
            />
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="doSearch" class="status-select">
                <el-option label="全部" :value="undefined" />
                <el-option label="已显示" :value="1" />
                <el-option label="已隐藏" :value="0" />
            </el-select>
            <el-button type="primary" @click="doSearch">搜索</el-button>
        </div>

        <el-table :data="reviewList" v-loading="loading" stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="60" align="center" />
            <el-table-column label="商品" min-width="180">
                <template #default="{ row }">
                    <div class="product-cell">
                        <el-image
                            class="product-img"
                            :src="getImageUrl(row.productImage)"
                            fit="cover"
                            v-if="row.productImage"
                        >
                            <template #error>
                                <div class="img-placeholder" />
                            </template>
                        </el-image>
                        <span class="product-title">{{ row.productTitle }}</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="buyerName" label="买家" width="100" align="center" />
            <el-table-column label="评分" width="160" align="center">
                <template #default="{ row }">
                    <el-rate :model-value="row.rating" disabled :max="5" size="small" />
                </template>
            </el-table-column>
            <el-table-column label="评价内容" min-width="200">
                <template #default="{ row }">
                    <div class="content-cell">{{ row.content }}</div>
                </template>
            </el-table-column>
            <el-table-column label="卖家回复" min-width="160">
                <template #default="{ row }">
                    <span v-if="row.replyContent" class="reply-text">{{ row.replyContent }}</span>
                    <span v-else class="no-reply">暂未回复</span>
                </template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                        {{ row.status === 1 ? '显示' : '隐藏' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="评价时间" width="160" align="center">
                <template #default="{ row }">
                    {{ row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '' }}
                </template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center" fixed="right">
                <template #default="{ row }">
                    <el-button
                        :type="row.status === 1 ? 'warning' : 'success'"
                        size="small"
                        @click="handleToggle(row)"
                        :loading="togglingId === row.id"
                    >
                        {{ row.status === 1 ? '隐藏' : '显示' }}
                    </el-button>
                </template>
            </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-bar" v-if="totalCount > 0">
            <el-pagination
                v-model:current-page="currentPage"
                :page-size="pageSize"
                :total="totalCount"
                layout="prev, pager, next"
                @current-change="loadReviews"
                background
            />
        </div>
    </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { reviewListPageApi, toggleReviewStatusApi } from '@/api/review-api'
import type { ProductReviewVO } from '@/types/review'

const loading = ref(false)
const reviewList = ref<ProductReviewVO[]>([])
const totalCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const togglingId = ref<number | null>(null)

const getImageUrl = (path?: string) => {
    if (!path) return ''
    if (path.startsWith('http://') || path.startsWith('https://')) return path
    return `${import.meta.env.VITE_API_BASE_URL}${path}`
}

const doSearch = () => {
    currentPage.value = 1
    loadReviews()
}

const loadReviews = async () => {
    loading.value = true
    try {
        const res = await reviewListPageApi({
            current: currentPage.value,
            size: pageSize.value,
            status: statusFilter.value,
            keyword: keyword.value || undefined
        })
        reviewList.value = res?.data ?? []
        totalCount.value = res?.count ?? 0
    } catch (e: any) {
        ElMessage.error(e.message || '加载评价列表失败')
    } finally {
        loading.value = false
    }
}

const handleToggle = async (row: ProductReviewVO) => {
    if (!row.id) return
    togglingId.value = row.id
    try {
        const newStatus = row.status === 1 ? 0 : 1
        await toggleReviewStatusApi(row.id, newStatus)
        ElMessage.success(newStatus === 1 ? '评价已显示' : '评价已隐藏')
        loadReviews()
    } catch (e: any) {
        ElMessage.error(e.message || '操作失败')
    } finally {
        togglingId.value = null
    }
}

// 初始加载
loadReviews()
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;
.review-management {
    padding: 24px;
}

.search-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 24px;

    .search-input {
        width: 280px;
    }

    .status-select {
        width: 140px;
    }
}

.product-cell {
    display: flex;
    align-items: center;
    gap: 10px;

    .product-img {
        width: 40px;
        height: 40px;
        border-radius: 4px;
        flex-shrink: 0;
        background: #f5f5f7;
    }

    .img-placeholder {
        width: 40px;
        height: 40px;
        background: #f5f5f7;
        border-radius: 4px;
    }

    .product-title {
        font-size: 13px;
        color: $color-text;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }
}

.content-cell {
    max-width: 300px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: $color-text-secondary;
    font-size: 13px;
}

.reply-text {
    font-size: 13px;
    color: $color-text-secondary;
}

.no-reply {
    font-size: 12px;
    color: $color-text-light;
}

.pagination-bar {
    display: flex;
    justify-content: center;
    padding-top: 24px;
}
</style>
