<template>
    <div class="create-order" v-loading="loading">
        <!-- 返回按钮 -->
        <div class="back-bar" @click="goBack">
            <el-icon :size="20"><ArrowLeft /></el-icon>
            <span>返回</span>
        </div>

        <template v-if="product">
            <div class="page-title">确认订单</div>

            <!-- ====== 商品信息 ====== -->
            <div class="section-card">
                <h3 class="section-title">商品信息</h3>
                <div class="product-info">
                    <el-image
                        class="product-img"
                        :src="productImage"
                        fit="cover"
                    >
                        <template #error>
                            <div class="img-placeholder">
                                <el-icon :size="24"><Picture /></el-icon>
                            </div>
                        </template>
                    </el-image>
                    <div class="product-detail">
                        <div class="product-title">{{ product.title }}</div>
                        <div class="product-meta">
                            <el-tag size="small" effect="plain" type="info">
                                {{ tradeTypeLabel(product.tradeType) }}
                            </el-tag>
                        </div>
                        <div class="product-price">¥{{ product.price }}</div>
                        <!-- 库存指标 -->
                        <div class="order-stock" v-if="product.stock != null">
                            <template v-if="product.stock > 0">
                                <el-tag size="small" :type="product.stock <= 5 ? 'warning' : 'success'" effect="plain">
                                    {{ product.stock <= 5 ? '库存紧张' : '有货' }}
                                </el-tag>
                                <span class="stock-count">库存 {{ product.stock }} 件</span>
                            </template>
                            <template v-else>
                                <el-tag size="small" type="info" effect="plain">无限量</el-tag>
                            </template>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ====== 数量与备注 ====== -->
            <div class="section-card">
                <h3 class="section-title">购买信息</h3>
                <div class="form-row">
                    <span class="form-label">购买数量</span>
                    <div class="quantity-wrap">
                        <el-input-number
                            v-model="form.quantity"
                            :min="1"
                            :max="maxQuantity"
                            size="large"
                        />
                        <el-alert
                            v-if="stockWarning"
                            :title="stockWarning"
                            type="warning"
                            :closable="false"
                            show-icon
                            class="stock-warning-msg"
                        />
                    </div>
                </div>
                <div class="form-row">
                    <span class="form-label">买家备注</span>
                    <el-input
                        v-model="form.buyerRemark"
                        placeholder="选填，给卖家留言"
                        maxlength="200"
                        show-word-limit
                        clearable
                    />
                </div>
            </div>

            <!-- ====== 收货方式/地址 ====== -->
            <div class="section-card">
                <h3 class="section-title">收货方式</h3>

                <!-- 自提模式 -->
                <div v-if="product.tradeType === 1" class="pickup-info">
                    <el-icon :size="20" color="#67c23a"><SuccessFilled /></el-icon>
                    <span>该商品为自提交易，下单后请与卖家沟通自提时间和地点</span>
                </div>

                <!-- 两者皆可：选择自提或邮寄 -->
                <div v-if="product.tradeType === 3" class="delivery-selector">
                    <div
                        :class="['delivery-option', { active: selectedDeliveryMode === 'pickup' }]"
                        @click="selectedDeliveryMode = 'pickup'"
                    >
                        <el-icon :size="28"><TakeawayBox /></el-icon>
                        <div class="option-text">
                            <span class="option-label">自提</span>
                            <span class="option-desc">自行与卖家沟通取货</span>
                        </div>
                    </div>
                    <div
                        :class="['delivery-option', { active: selectedDeliveryMode === 'shipping' }]"
                        @click="selectedDeliveryMode = 'shipping'"
                    >
                        <el-icon :size="28"><Van /></el-icon>
                        <div class="option-text">
                            <span class="option-label">邮寄</span>
                            <span class="option-desc">需选择收货地址（运费 ¥6）</span>
                        </div>
                    </div>
                </div>

                <!-- 邮寄模式 | 两者皆可选邮寄 -->
                <template v-if="product.tradeType === 2 || (product.tradeType === 3 && selectedDeliveryMode === 'shipping')">
                    <div v-if="selectedAddress" class="address-card address-selected" @click="selectAddress">
                        <div class="address-info">
                            <div class="address-header">
                                <span class="receiver">{{ selectedAddress.receiverName }}</span>
                                <span class="phone">{{ selectedAddress.receiverPhone }}</span>
                                <el-tag v-if="selectedAddress.isDefault === 1" size="small" type="warning">默认</el-tag>
                            </div>
                            <div class="address-detail">
                                {{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.district }}{{ selectedAddress.detailAddress }}
                            </div>
                        </div>
                        <el-icon class="arrow-icon"><ArrowRight /></el-icon>
                    </div>
                    <div v-else class="address-card address-empty" @click="selectAddress">
                        <el-icon :size="20"><Plus /></el-icon>
                        <span>请选择收货地址</span>
                    </div>
                </template>
            </div>

            <!-- ====== 费用明细 ====== -->
            <div class="section-card">
                <h3 class="section-title">费用明细</h3>
                <div class="fee-row">
                    <span>商品金额</span>
                    <span>¥{{ (product.price! * form.quantity).toFixed(2) }}</span>
                </div>
                <div class="fee-row" v-if="shippingFee > 0">
                    <span>运费</span>
                    <span>¥{{ shippingFee.toFixed(2) }}</span>
                </div>
                <div class="fee-row" v-else>
                    <span>运费</span>
                    <span class="free">免运费</span>
                </div>
                <div class="fee-row total">
                    <span>实付金额</span>
                    <span class="total-price">¥{{ payAmount.toFixed(2) }}</span>
                </div>
            </div>

            <!-- ====== 提交按钮 ====== -->
            <div class="submit-bar">
                <el-button
                    type="primary"
                    size="large"
                    :loading="submitting"
                    :disabled="!canSubmit"
                    @click="handleSubmit"
                    round
                >
                    提交订单
                </el-button>
            </div>
        </template>

        <!-- 商品不存在 -->
        <el-empty v-else-if="!loading" description="商品不存在或已下架" />

        <!-- ====== 选择地址弹窗 ====== -->
        <el-dialog
            v-model="dialogAddressVisible"
            title="选择收货地址"
            width="520px"
            destroy-on-close
        >
            <div class="address-list" v-loading="addressLoading">
                <div
                    v-for="addr in addressList"
                    :key="addr.id"
                    :class="['address-item', { selected: tempSelectedAddress?.id === addr.id }]"
                    @click="chooseAddress(addr)"
                >
                    <div class="addr-header">
                        <span class="addr-name">{{ addr.receiverName }}</span>
                        <span class="addr-phone">{{ addr.receiverPhone }}</span>
                        <el-tag v-if="addr.isDefault === 1" size="small" type="warning">默认</el-tag>
                    </div>
                    <div class="addr-detail">
                        {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
                    </div>
                    <el-icon v-if="tempSelectedAddress?.id === addr.id" class="check-icon" color="#409eff"><CircleCheck /></el-icon>
                </div>
                <el-empty v-if="!addressLoading && addressList.length === 0" description="暂无收货地址，请先添加" />
            </div>
            <template #footer>
                <el-button @click="dialogAddressVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmAddress">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { ref, computed, watch, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight, Picture, Plus, SuccessFilled, CircleCheck, TakeawayBox, Van } from '@element-plus/icons-vue'
import { getProductDetailApi } from '@/api/product-api'
import { listAddressApi } from '@/api/address-api'
import { createOrderApi } from '@/api/order-api'
import type { ProductVO } from '@/types/product'
import type { AddressVO } from '@/types/address'
import type { OrderCreateDTO } from '@/types/order'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const product = ref<ProductVO | null>(null)

// 表单
const form = reactive<OrderCreateDTO>({
    productId: undefined,
    addressId: undefined,
    quantity: 1,
    buyerRemark: ''
})

// 地址选择
const dialogAddressVisible = ref(false)
const addressLoading = ref(false)
const addressList = ref<AddressVO[]>([])
const selectedAddress = ref<AddressVO | null>(null)
const tempSelectedAddress = ref<AddressVO | null>(null)

// 两者皆可模式下的选择
const selectedDeliveryMode = ref<'pickup' | 'shipping'>('pickup')

// 切换为自提时清除已选地址
watch(selectedDeliveryMode, (mode) => {
    if (mode === 'pickup') {
        selectedAddress.value = null
        form.addressId = undefined
    }
})

// 商品首图
const productImage = computed(() => {
    const img = product.value?.images?.[0]
    if (!img?.imageUrl) return ''
    return getImageUrl(img.imageUrl)
})

// 运费（邮寄模式固定6元，含 tradeType=3 选择邮寄）
const shippingFee = computed(() => {
    const tradeType = product.value?.tradeType
    if (tradeType === 2) return 6
    if (tradeType === 3 && selectedDeliveryMode.value === 'shipping') return 6
    return 0
})

// 实付金额
const payAmount = computed(() => {
    const price = product.value?.price ?? 0
    return price * form.quantity + shippingFee.value
})

// 最大可购买数量（限库存时取库存数，不限时取99）
const maxQuantity = computed(() => {
    const stock = product.value?.stock
    if (stock != null && stock > 0) return stock
    return 99
})

// 库存不足警告
const stockWarning = computed(() => {
    const stock = product.value?.stock
    const qty = form.quantity ?? 1
    if (stock != null && stock > 0 && qty > stock) {
        return `当前数量超出库存（仅剩 ${stock} 件），请调整`
    }
    if (stock != null && stock > 0 && stock <= 5) {
        return `库存紧张，仅剩 ${stock} 件`
    }
    return ''
})

// 能否提交
const canSubmit = computed(() => {
    if (!product.value) return false
    const tradeType = product.value?.tradeType
    // 邮寄或两者皆可选邮寄需要选择地址
    if (tradeType === 2) {
        return selectedAddress.value !== null
    }
    if (tradeType === 3 && selectedDeliveryMode.value === 'shipping') {
        return selectedAddress.value !== null
    }
    return true
})

// 工具函数
const getImageUrl = (path?: string) => {
    if (!path) return ''
    if (path.startsWith('http://') || path.startsWith('https://')) return path
    return `${import.meta.env.VITE_API_BASE_URL}${path}`
}

const tradeTypeLabel = (v?: number) => {
    const map: Record<number, string> = { 1: '自提', 2: '邮寄', 3: '两者皆可' }
    return v ? map[v] || '' : ''
}

const goBack = () => {
    router.back()
}

// 选择地址弹窗
const selectAddress = async () => {
    dialogAddressVisible.value = true
    tempSelectedAddress.value = selectedAddress.value ? { ...selectedAddress.value } : null
    if (addressList.value.length === 0) {
        await loadAddresses()
    }
}

const loadAddresses = async () => {
    addressLoading.value = true
    try {
        const res = await listAddressApi()
        addressList.value = res?.data ?? []
        // 如果有默认地址且未选择，自动选中
        if (!selectedAddress.value) {
            const defaultAddr = addressList.value.find(a => a.isDefault === 1)
            if (defaultAddr) {
                tempSelectedAddress.value = defaultAddr
            }
        }
    } catch {
        // 地址加载失败不影响主流程
    } finally {
        addressLoading.value = false
    }
}

const chooseAddress = (addr: AddressVO) => {
    tempSelectedAddress.value = addr
}

const confirmAddress = () => {
    if (!tempSelectedAddress.value) {
        ElMessage.warning('请选择一个收货地址')
        return
    }
    selectedAddress.value = tempSelectedAddress.value
    form.addressId = selectedAddress.value.id
    dialogAddressVisible.value = false
}

// 提交订单
const handleSubmit = async () => {
    form.productId = product.value?.id
    if (!form.productId) return

    submitting.value = true
    try {
        // tradeType=3 选择自提时不传地址
        const tradeType = product.value?.tradeType
        const addrId = (tradeType === 3 && selectedDeliveryMode.value === 'pickup') ? undefined : form.addressId
        const res = await createOrderApi({
            productId: form.productId,
            addressId: addrId,
            quantity: form.quantity,
            buyerRemark: form.buyerRemark || undefined
        })
        ElMessage.success('下单成功！')
        // 跳转到订单详情页
        const orderId = res?.data?.id
        if (orderId) {
            router.push('/user-home/orders')
        } else {
            router.push('/user-home/orders')
        }
    } catch (e: any) {
        ElMessage.error(e.message || '下单失败')
    } finally {
        submitting.value = false
    }
}

// 加载商品数据
onMounted(async () => {
    const id = Number(route.params.id)
    if (!id || isNaN(id)) {
        ElMessage.error('商品ID无效')
        return
    }
    loading.value = true
    try {
        const res = await getProductDetailApi(id)
        product.value = res?.data ?? null
        form.productId = id
    } catch (e: any) {
        ElMessage.error(e.message || '加载商品信息失败')
    } finally {
        loading.value = false
    }
})
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;
.create-order {
    max-width: 640px;
    margin: 0 auto;
    padding-bottom: $spacing-xl;
}

.back-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: $spacing-lg 0;
    color: $color-text-secondary;
    font-size: 15px;
    cursor: pointer;
    user-select: none;
    transition: color 0.2s;

    &:hover {
        color: $color-primary;
    }
}

.page-title {
    font-size: 24px;
    font-weight: 700;
    color: $color-text;
    margin-bottom: $spacing-lg;
}

// ====== 区块卡片 ======
.section-card {
    background: $color-surface;
    border-radius: $radius-lg;
    padding: $spacing-xl;
    margin-bottom: $spacing-lg;
    box-shadow: $shadow-sm;
}

.section-title {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $color-text;
    margin-bottom: $spacing-md;
    padding-bottom: $spacing-sm;
    border-bottom: 1px solid $color-border-light;
}

// ====== 商品信息 ======
.product-info {
    display: flex;
    gap: $spacing-md;

    .product-img {
        width: 100px;
        height: 100px;
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

    .product-detail {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 10px;

        .product-title {
            font-size: 16px;
            font-weight: 600;
            color: $color-text;
            line-height: 1.5;
        }

        .product-meta {
            display: flex;
            gap: 8px;
        }

        .product-price {
            font-size: 22px;
            font-weight: 700;
            color: $color-accent;
        }
    }
}

// ====== 表单行 ======
.form-row {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .form-label {
        width: 80px;
        flex-shrink: 0;
        font-size: 15px;
        color: $color-text-secondary;
        font-weight: 500;
    }

    .el-input {
        flex: 1;
    }
}

// ====== 自提提示 ======
.pickup-info {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 18px;
    background: #f0f9eb;
    border-radius: $radius-md;
    color: #606266;
    font-size: 14px;
    line-height: 1.6;
}

// ====== 两者皆可选择器 ======
.delivery-selector {
    display: flex;
    gap: 14px;
    margin-bottom: 24px;

    .delivery-option {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 18px;
        border: 2px solid $color-border;
        border-radius: $radius-md;
        cursor: pointer;
        transition: all 0.25s;
        user-select: none;

        &:hover {
            border-color: $color-primary;
            background: rgba($color-primary, 0.03);
        }

        &.active {
            border-color: $color-primary;
            background: rgba($color-primary, 0.06);
            box-shadow: 0 0 0 1px $color-primary;

            .el-icon {
                color: $color-primary;
            }

            .option-label {
                color: $color-primary;
                font-weight: 700;
            }
        }

        .option-text {
            display: flex;
            flex-direction: column;
            gap: 4px;

            .option-label {
                font-size: 16px;
                font-weight: 600;
                color: $color-text;
                transition: color 0.25s;
            }

            .option-desc {
                font-size: 13px;
                color: $color-text-light;
            }
        }

        .el-icon {
            color: $color-text-light;
            transition: color 0.25s;
        }
    }
}

// ====== 已选中地址样式（蓝色区分）======
.address-selected {
    border-color: $color-primary !important;
    background: rgba($color-primary, 0.05) !important;
    box-shadow: 0 0 0 1px $color-primary;

    .receiver {
        color: $color-primary !important;
    }
}

// ====== 收货地址卡片 ======
.address-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 18px;
    border: 1px solid $color-border;
    border-radius: $radius-md;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
        border-color: $color-primary;
        background: rgba($color-primary, 0.02);
    }

    .address-info {
        flex: 1;
    }

    .address-header {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        .receiver {
            font-weight: 600;
            color: $color-text;
            font-size: 15px;
        }

        .phone {
            color: $color-text-secondary;
            font-size: 14px;
        }
    }

    .address-detail {
        font-size: 14px;
        color: $color-text-secondary;
        line-height: 1.5;
    }

    .arrow-icon {
        color: $color-text-light;
        flex-shrink: 0;
    }

    &.address-empty {
        display: flex;
        align-items: center;
        gap: 8px;
        color: $color-text-light;
        font-size: 15px;

        &:hover {
            color: $color-primary;
        }
    }
}

// ====== 费用明细 ======
.fee-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    font-size: 15px;
    color: $color-text-secondary;

    .free {
        color: #67c23a;
    }

    &.total {
        padding-top: 14px;
        margin-top: 10px;
        border-top: 1px solid $color-border-light;
        font-size: 16px;
        font-weight: 600;
        color: $color-text;

        .total-price {
            color: $color-accent;
            font-size: 24px;
            font-weight: 800;
        }
    }
}

// ====== 提交按钮栏 ======
.submit-bar {
    display: flex;
    justify-content: center;
    padding: $spacing-xl 0 $spacing-2xl;

    .el-button {
        width: 300px;
        height: 56px;
        font-size: 18px;
        font-weight: 600;
        padding: 14px 0;
        letter-spacing: 2px;
    }
}

// ====== 地址选择列表 ======
.address-list {
    max-height: 400px;
    overflow-y: auto;
}

.address-item {
    position: relative;
    padding: 16px 18px;
    border: 1px solid $color-border;
    border-radius: $radius-md;
    margin-bottom: 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
        border-color: $color-primary;
    }

    &.selected {
        border-color: $color-primary;
        background: rgba($color-primary, 0.03);
    }

    .addr-header {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        .addr-name {
            font-weight: 600;
            color: $color-text;
            font-size: 15px;
        }

        .addr-phone {
            color: $color-text-secondary;
            font-size: 14px;
        }
    }

    .addr-detail {
        font-size: 14px;
        color: $color-text-secondary;
        line-height: 1.5;
        padding-right: 30px;
    }

    .check-icon {
        position: absolute;
        right: 14px;
        top: 50%;
        transform: translateY(-50%);
        font-size: 22px;
    }
}

// ====== 库存指标 ======
.order-stock {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid $color-border-light;

    .stock-count {
        font-size: 14px;
        color: $color-text-secondary;
    }
}

// ====== 数量与库存警告 ======
.quantity-wrap {
    flex: 1;

    .stock-warning-msg {
        margin-top: 10px;
        padding: 8px 14px;
    }
}
</style>
