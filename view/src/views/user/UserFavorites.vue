<template>
    <div class="user-favorites">
        <div class="page-header">
            <span class="total-count" v-if="total > 0">共 {{ total }} 件收藏</span>
        </div>

        <!-- 收藏列表（卡片网格） -->
        <div class="favorites-grid" v-loading="loading">
            <div
                v-for="item in list"
                :key="item.id"
                class="fav-card"
                @click="goDetail(item.id)"
            >
                <div class="card-image">
                    <el-image
                        :src="getImageUrl(coverImage(item))"
                        fit="cover"
                        class="fav-img"
                    >
                        <template #error>
                            <div class="img-placeholder">
                                <el-icon :size="28"><Picture /></el-icon>
                            </div>
                        </template>
                    </el-image>
                    <span class="card-price">¥{{ item.price }}</span>
                </div>
                <div class="card-info">
                    <p class="card-title">{{ item.title || '未命名商品' }}</p>
                    <div class="card-meta">
                        <span class="meta-time">{{ formatTime(item.createTime) }}</span>
                    </div>
                </div>
            </div>

            <!-- 空状态 -->
            <el-empty v-if="!loading && list.length === 0" description="还没有收藏的商品" />
        </div>

        <!-- 分页 -->
        <div class="pagination-wrap" v-if="total > size">
            <el-pagination
                v-model:current-page="current"
                v-model:page-size="size"
                :total="total"
                layout="prev, pager, next, total"
                @current-change="fetchData"
            />
        </div>
    </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { listFavoritesApi } from '@/api/product-api'
import type { ProductVO } from '@/types/product'

const router = useRouter()

const list = ref<ProductVO[]>([])
const loading = ref(false)
const total = ref(0)
const current = ref(1)
const size = ref(10)

const getImageUrl = (path?: string) => {
    if (!path) return ''
    if (path.startsWith('http://') || path.startsWith('https://')) return path
    return `${import.meta.env.VITE_API_BASE_URL}${path}`
}

const coverImage = (item: ProductVO) => {
    const cover = item.images?.find(img => img.isCover === 1)
    return cover?.imageUrl || item.images?.[0]?.imageUrl || ''
}

const formatTime = (t?: string) => {
    if (!t) return ''
    const date = new Date(t)
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    const days = Math.floor(diff / 86400000)
    if (days < 1) return '今天'
    if (days < 7) return `${days}天前`
    if (days < 30) return `${Math.floor(days / 7)}周前`
    return date.toLocaleDateString('zh-CN')
}

const goDetail = (id?: number) => {
    if (id) {
        router.push(`/user-home/product-detail/${id}`)
    }
}

const fetchData = async () => {
    loading.value = true
    try {
        const params = { current: current.value, size: size.value }
        const res = await listFavoritesApi(params)
        list.value = res?.data ?? []
        total.value = res?.count ?? 0
    } catch (e: any) {
        ElMessage.error(e.message || '加载收藏列表失败')
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    fetchData()
})
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;
.user-favorites {
    padding: 0 0 $spacing-xl;
}

.page-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: $spacing-lg;

    h3 {
        font-size: 20px;
        font-weight: 600;
        color: $color-text;
        margin: 0;
    }

    .total-count {
        font-size: $font-size-sm;
        color: $color-text-light;
    }
}

// ====== 收藏卡片网格 ======
.favorites-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 16px;
}

.fav-card {
    background: $color-surface;
    border-radius: $radius-lg;
    overflow: hidden;
    cursor: pointer;
    box-shadow: $shadow-sm;
    transition: transform 0.25s, box-shadow 0.25s;

    &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.10);
    }

    .card-image {
        position: relative;
        aspect-ratio: 1 / 1;
        background: #f5f5f7;

        .fav-img {
            width: 100%;
            height: 100%;
        }

        .img-placeholder {
            width: 100%;
            height: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #bbb;
            background: #f5f5f7;
        }

        .card-price {
            position: absolute;
            bottom: 8px;
            left: 8px;
            background: rgba(0, 0, 0, 0.55);
            color: #fff;
            font-size: 13px;
            font-weight: 700;
            padding: 2px 10px;
            border-radius: 6px;
            backdrop-filter: blur(4px);
        }
    }

    .card-info {
        padding: $spacing-sm $spacing-md $spacing-md;

        .card-title {
            font-size: $font-size-base;
            font-weight: 500;
            color: $color-text;
            margin: 0 0 6px;
            line-height: 1.4;
            overflow: hidden;
            text-overflow: ellipsis;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
        }

        .card-meta {
            display: flex;
            align-items: center;
            gap: 8px;

            .meta-time {
                font-size: $font-size-xs;
                color: $color-text-light;
            }
        }
    }
}

// ====== 分页 ======
.pagination-wrap {
    display: flex;
    justify-content: center;
    margin-top: $spacing-lg;
}
</style>
