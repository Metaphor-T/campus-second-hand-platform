<template>
    <div class="add-review">
        <div class="page-header">
            <el-button :icon="ArrowLeft" @click="goBack" text class="back-btn">返回</el-button>
            <h2 class="page-title">发表评价</h2>
        </div>

        <div v-loading="loading" class="review-content">
            <!-- 订单商品信息 -->
            <div class="order-info" v-if="order">
                <el-image
                    class="product-img"
                    :src="getImageUrl(order.productImage)"
                    fit="cover"
                >
                    <template #error>
                        <div class="img-placeholder">
                            <el-icon :size="20"><Picture /></el-icon>
                        </div>
                    </template>
                </el-image>
                <div class="product-info">
                    <div class="product-title">{{ order.productTitle }}</div>
                    <div class="product-meta">
                        <span class="price">¥{{ order.unitPrice }}</span>
                        <span class="quantity">× {{ order.quantity }}</span>
                    </div>
                    <div class="order-no">订单号：{{ order.orderNo }}</div>
                </div>
            </div>

            <!-- 评分 -->
            <div class="rating-section">
                <span class="section-label">商品评分</span>
                <el-rate
                    v-model="form.rating"
                    :max="5"
                    :texts="['非常差', '较差', '一般', '满意', '非常满意']"
                    show-text
                    allow-half
                    style="display: inline-block;"
                />
            </div>

            <!-- 评价内容 -->
            <div class="content-section">
                <span class="section-label">评价内容</span>
                <el-input
                    v-model="form.content"
                    type="textarea"
                    :rows="5"
                    maxlength="500"
                    show-word-limit
                    placeholder="说说你的使用心得，分享给其他买家参考吧"
                />
            </div>

            <!-- 图片上传 -->
            <div class="image-section">
                <span class="section-label">上传图片</span>
                <div class="image-upload-area">
                    <div
                        v-for="(img, index) in uploadedImages"
                        :key="index"
                        class="uploaded-image-wrapper"
                    >
                        <el-image class="uploaded-image" :src="getImageUrl(img)" fit="cover" />
                        <el-button
                            class="remove-btn"
                            circle
                            size="small"
                            type="danger"
                            @click="removeImage(index)"
                        >
                            <el-icon><Close /></el-icon>
                        </el-button>
                    </div>
                    <el-upload
                        v-if="uploadedImages.length < 9"
                        :action="uploadUrl"
                        :on-success="onImageUpload"
                        :before-upload="beforeImageUpload"
                        :show-file-list="false"
                        class="upload-trigger"
                        accept="image/jpeg,image/png,image/gif"
                    >
                        <el-icon :size="28"><Plus /></el-icon>
                    </el-upload>
                </div>
                <p class="upload-tip">最多上传 9 张，支持 JPG/PNG，单张不超过 2MB</p>
            </div>

            <!-- 提交按钮 -->
            <div class="submit-section">
                <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit" :disabled="!canSubmit">
                    提交评价
                </el-button>
            </div>
        </div>
    </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Picture, Plus, Close } from '@element-plus/icons-vue'
import type { UploadProps } from 'element-plus'
import { getOrderDetailApi } from '@/api/order-api'
import { addReviewApi } from '@/api/review-api'
import type { OrderVO } from '@/types/order'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const order = ref<OrderVO | null>(null)
const uploadedImages = ref<string[]>([])
const uploadUrl = `${import.meta.env.VITE_API_BASE_URL}/files/upload`

const form = reactive({
    rating: 5,
    content: ''
})

const canSubmit = computed(() => {
    return form.rating > 0 && form.content.trim().length > 0 && !submitting.value
})

const getImageUrl = (path?: string) => {
    if (!path) return ''
    if (path.startsWith('http://') || path.startsWith('https://')) return path
    return `${import.meta.env.VITE_API_BASE_URL}${path}`
}

const goBack = () => {
    router.back()
}

// 加载订单信息
const loadOrder = async () => {
    const orderId = Number(route.params.orderId)
    if (!orderId) {
        ElMessage.error('参数错误')
        router.back()
        return
    }
    loading.value = true
    try {
        const res = await getOrderDetailApi(orderId)
        order.value = res?.data ?? null
        if (!order.value) {
            ElMessage.error('订单不存在')
            router.back()
        }
    } catch (e: any) {
        ElMessage.error(e.message || '加载订单失败')
        router.back()
    } finally {
        loading.value = false
    }
}

// 图片上传
const onImageUpload: UploadProps['onSuccess'] = (response) => {
    if (response?.code === 200 && response?.data?.filePath) {
        uploadedImages.value.push(response.data.filePath)
    } else {
        ElMessage.error('图片上传失败')
    }
}

const beforeImageUpload: UploadProps['beforeUpload'] = (rawFile) => {
    const isImage = ['image/jpeg', 'image/png', 'image/gif'].includes(rawFile.type)
    if (!isImage) {
        ElMessage.error('仅支持 JPG/PNG/GIF 格式图片')
        return false
    }
    const isLt2M = rawFile.size / 1024 / 1024 < 2
    if (!isLt2M) {
        ElMessage.error('图片大小不能超过 2MB')
        return false
    }
    return true
}

const removeImage = (index: number) => {
    uploadedImages.value.splice(index, 1)
}

// 提交评价
const handleSubmit = async () => {
    if (!canSubmit.value) return
    const orderId = Number(route.params.orderId)
    submitting.value = true
    try {
        await addReviewApi({
            orderId: orderId,
            rating: form.rating,
            content: form.content.trim(),
            images: uploadedImages.value.length > 0 ? uploadedImages.value : undefined
        })
        ElMessage.success('评价成功！')
        router.push('/user-home/orders?status=3')
    } catch (e: any) {
        ElMessage.error(e.message || '提交评价失败')
    } finally {
        submitting.value = false
    }
}

onMounted(() => {
    loadOrder()
})
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;
.add-review {
    max-width: 640px;
    margin: 0 auto;
    padding-bottom: $spacing-xl;
}

.page-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: $spacing-lg;

    .back-btn {
        font-size: 14px;
        padding: 4px 8px;
    }

    .page-title {
        font-size: 22px;
        font-weight: 700;
        color: $color-text;
    }
}

// ====== 订单信息卡片 ======
.order-info {
    display: flex;
    gap: $spacing-md;
    padding: $spacing-md;
    background: $color-surface;
    border-radius: $radius-lg;
    box-shadow: $shadow-sm;
    margin-bottom: $spacing-lg;

    .product-img {
        width: 80px;
        height: 80px;
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

    .product-info {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 6px;
        min-width: 0;

        .product-title {
            font-size: $font-size-base;
            font-weight: 600;
            color: $color-text;
        }

        .product-meta {
            display: flex;
            gap: 8px;
            align-items: center;

            .price {
                font-size: $font-size-base;
                color: $color-accent;
                font-weight: 600;
            }

            .quantity {
                font-size: $font-size-sm;
                color: $color-text-light;
            }
        }

        .order-no {
            font-size: $font-size-xs;
            color: $color-text-light;
            font-family: 'Courier New', monospace;
        }
    }
}

// ====== 各区块 ======
.section-label {
    display: block;
    font-size: 15px;
    font-weight: 600;
    color: $color-text;
    margin-bottom: 12px;
}

.rating-section {
    background: $color-surface;
    border-radius: $radius-lg;
    padding: $spacing-lg;
    margin-bottom: $spacing-md;
    box-shadow: $shadow-sm;
}

.content-section {
    background: $color-surface;
    border-radius: $radius-lg;
    padding: $spacing-lg;
    margin-bottom: $spacing-md;
    box-shadow: $shadow-sm;
}

.image-section {
    background: $color-surface;
    border-radius: $radius-lg;
    padding: $spacing-lg;
    margin-bottom: $spacing-lg;
    box-shadow: $shadow-sm;

    .image-upload-area {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;

        .uploaded-image-wrapper {
            position: relative;
            width: 80px;
            height: 80px;

            .uploaded-image {
                width: 100%;
                height: 100%;
                border-radius: $radius-md;
                object-fit: cover;
            }

            .remove-btn {
                position: absolute;
                top: -8px;
                right: -8px;
                width: 20px;
                height: 20px;
            }
        }

        .upload-trigger {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 80px;
            height: 80px;
            border: 1px dashed #d9d9d9;
            border-radius: $radius-md;
            cursor: pointer;
            color: #999;
            background: #fafafa;
            transition: all 0.2s;

            &:hover {
                border-color: $color-primary;
                color: $color-primary;
                background: rgba($color-primary, 0.03);
            }
        }
    }

    .upload-tip {
        margin-top: 8px;
        font-size: 12px;
        color: #909399;
    }
}

// ====== 提交 ======
.submit-section {
    display: flex;
    justify-content: center;
    padding-top: $spacing-md;
}
</style>
