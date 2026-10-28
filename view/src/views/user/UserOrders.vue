<template>
    <div class="user-orders">
        <!-- 角色切换（买家/卖家，胶囊风格） -->
        <div class="role-tabs">
            <div class="tab-panel">
                <span
                    v-for="role in roleTabs"
                    :key="role.value"
                    :class="['tab-item', { active: activeRole === role.value }]"
                    @click="handleRoleSwitch(role.value as 'buyer' | 'seller')"
                >
                    {{ role.label }}
                </span>
            </div>
        </div>

        <!-- 状态筛选（el-tabs 白色卡片风格） -->
        <el-tabs v-model="activeStatusStr" class="status-tabs" @tab-change="onStatusChange">
            <el-tab-pane
                v-for="tab in statusTabsWithCount"
                :key="tab.value"
                :name="tab.name"
                :label="tab.label"
            />
        </el-tabs>

        <!-- 搜索栏 -->
        <div class="search-bar">
            <div class="search-wrapper">
                <el-icon class="search-icon"><Search /></el-icon>
                <el-input
                    v-model="keyword"
                    placeholder="搜索订单编号"
                    clearable
                    @clear="doSearch"
                    @keyup.enter="doSearch"
                    class="search-input"
                />
                <el-button class="search-btn" @click="doSearch">搜索</el-button>
            </div>
        </div>

        <!-- 订单列表 -->
        <div v-loading="loading" class="order-list">
            <div v-if="orderList.length === 0 && !loading" class="empty-state">
                <el-empty :description="emptyText" />
            </div>

            <div v-for="order in orderList" :key="order.id" class="order-card">
                <!-- 订单头部：编号 -->
                <div class="order-header">
                    <div class="order-no">
                        <span class="label">订单编号：</span>
                        <span class="value">{{ order.orderNo }}</span>
                    </div>
                </div>

                <!-- 订单商品信息 + 操作 -->
                <div class="order-body">
                    <!-- 商品封面 + 状态标签 -->
                    <div class="product-img-wrapper">
                        <el-image
                            :key="order.productImage"
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
                        <el-tag
                            class="status-overlay"
                            :type="statusTagType(order.orderStatus)"
                            effect="dark"
                            size="small"
                        >
                            {{ statusLabel(order.orderStatus) }}
                        </el-tag>
                        <el-tag class="create-time-tag" size="small" type="info" effect="plain">
                            {{ formatTime(order.createTime) }}
                        </el-tag>
                    </div>
                    <div class="product-info">
                        <div class="product-title">{{ order.productTitle }}</div>
                        <div class="product-meta">
                            <span class="price">¥{{ order.unitPrice }}</span>
                            <span class="quantity">× {{ order.quantity }}</span>
                        </div>
                        <div class="trade-partner" v-if="activeRole === 'buyer'">
                            <el-tag size="small" type="success" effect="plain">卖家：{{ order.sellerName }}</el-tag>
                        </div>
                        <div class="trade-partner" v-else>
                            <el-tag size="small" type="success" effect="plain">买家：{{ order.buyerName }}</el-tag>
                        </div>
                    </div>
                    <div class="order-right">
                        <div class="order-amount">
                            <div class="amount">¥{{ order.payAmount }}</div>
                            <div class="fee" v-if="order.shippingFee && order.shippingFee > 0">
                                (含运费 ¥{{ order.shippingFee }})
                            </div>
                        </div>
                        <div class="order-actions">
                            <!-- 买家操作 -->
                            <template v-if="activeRole === 'buyer'">
                                <template v-if="order.orderStatus === 0">
                                    <el-button type="primary" size="small" @click="handlePay(order)">立即支付</el-button>
                                    <el-button size="small" @click="handleCancel(order)">取消订单</el-button>
                                </template>
                                <template v-if="order.orderStatus === 1">
                                    <el-button size="small" disabled>等待卖家发货</el-button>
                                </template>
                                <template v-if="order.orderStatus === 2">
                                    <el-button type="success" size="small" @click="handleReceive(order)">确认收货</el-button>
                                    <el-button size="small" @click="handleRefund(order)">申请退款</el-button>
                                </template>
                                <template v-if="order.orderStatus === 3">
                                    <el-button
                                        v-if="reviewedMap[order.id!] === true"
                                        size="small"
                                        disabled
                                    >
                                        已评价
                                    </el-button>
                                    <el-button
                                        v-else
                                        type="warning"
                                        size="small"
                                        @click="handleReview(order)"
                                    >
                                        去评价
                                    </el-button>
                                    <el-button size="small" @click="handleRefund(order)">申请售后</el-button>
                                </template>
                                <template v-if="order.orderStatus === 4 || order.orderStatus === 5">
                                    <el-button size="small" disabled>已完成</el-button>
                                </template>
                            </template>

                            <!-- 卖家操作 -->
                            <template v-if="activeRole === 'seller'">
                                <template v-if="order.orderStatus === 0">
                                    <el-button size="small" disabled>等待买家付款</el-button>
                                </template>
                                <template v-if="order.orderStatus === 1">
                                    <el-button type="primary" size="small" @click="handleShip(order)">立即发货</el-button>
                                </template>
                                <template v-if="order.orderStatus === 2">
                                    <el-button size="small" disabled>等待买家收货</el-button>
                                </template>
                                <template v-if="order.orderStatus === 3">
                                    <el-button size="small" type="primary" @click="handleViewReview(order)">查看评价</el-button>
                                </template>
                                <template v-if="order.orderStatus === 4">
                                    <el-button size="small" disabled>买家已取消</el-button>
                                </template>
                                <template v-if="order.orderStatus === 5">
                                    <el-button type="warning" size="small" @click="handleRefundAgree(order)">同意退款</el-button>
                                </template>
                            </template>
                        </div>
                    </div>
                </div>

                <!-- 地址信息（邮寄订单） -->
                <div class="address-info" v-if="order.fullAddress">
                    <span>{{ order.receiverName }} {{ order.receiverPhone }}</span>
                    <span class="address-text">{{ order.fullAddress }}</span>
                </div>

                <!-- 买家备注 -->
                <div class="remark-info" v-if="order.buyerRemark">
                    <span>备注：{{ order.buyerRemark }}</span>
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
                @current-change="loadOrders"
                size="large"
            />
        </div>

        <!-- ====== 卖家查看评价弹窗 ====== -->
        <el-dialog
            v-model="dialogReviewVisible"
            :title="'商品评价 - ' + (reviewDialogProductTitle || '')"
            width="520px"
            destroy-on-close
        >
            <div v-loading="reviewDialogLoading" class="review-dialog-body">
                <div v-if="reviewDialogList.length === 0 && !reviewDialogLoading" class="review-empty">
                    <el-empty description="暂无评价" :image-size="60" />
                </div>
                <div v-for="review in reviewDialogList" :key="review.id" class="review-dialog-item">
                    <div class="dialog-review-header">
                        <el-avatar :size="28" v-if="review.buyerAvatar" :src="getImageUrl(review.buyerAvatar)" />
                        <el-avatar :size="28" v-else>{{ (review.buyerName ?? '?')[0] }}</el-avatar>
                        <span class="dialog-review-buyer">{{ review.buyerName || '匿名用户' }}</span>
                        <el-rate :model-value="review.rating" disabled :max="5" size="small" />
                    </div>
                    <div class="dialog-review-content">{{ review.content }}</div>
                    <!-- 评价图片 -->
                    <div class="dialog-review-images" v-if="review.images && review.images.length > 0">
                        <el-image
                            v-for="(img, idx) in review.images"
                            :key="idx"
                            class="dialog-review-img"
                            :src="getImageUrl(img)"
                            fit="cover"
                        />
                    </div>
                    <!-- 已有回复 -->
                    <div class="dialog-seller-reply" v-if="review.replyContent">
                        <span class="reply-label">我的回复：</span>
                        <span class="reply-text">{{ review.replyContent }}</span>
                    </div>
                    <!-- 回复编辑 -->
                    <div class="dialog-reply-editor" v-else>
                        <el-input
                            v-model="replyTextMap[review.id!]"
                            type="textarea"
                            :rows="2"
                            maxlength="500"
                            show-word-limit
                            placeholder="请输入回复内容..."
                            size="small"
                        />
                        <el-button
                            type="primary"
                            size="small"
                            @click="handleReply(review)"
                            :loading="replyLoadingId === review.id"
                            :disabled="!replyTextMap[review.id!]?.trim()"
                            style="margin-top: 8px;"
                        >
                            回复
                        </el-button>
                    </div>
                </div>
            </div>
        </el-dialog>

        <!-- ====== 支付方式弹窗 ====== -->
        <el-dialog
            v-model="dialogPayVisible"
            title="选择支付方式"
            width="360px"
            destroy-on-close
            center
        >
            <div class="pay-methods">
                <div
                    v-for="method in payMethods"
                    :key="method.value"
                    :class="['pay-method-item', { selected: selectedPayMethod === method.value }]"
                    @click="selectedPayMethod = method.value"
                >
                    <span class="pay-icon">{{ method.icon }}</span>
                    <span class="pay-name">{{ method.label }}</span>
                    <el-icon v-if="selectedPayMethod === method.value" color="#409eff"><CircleCheck /></el-icon>
                </div>
            </div>
            <template #footer>
                <el-button @click="dialogPayVisible = false">取消</el-button>
                <el-button type="primary" :loading="payLoading" @click="confirmPay">确认支付</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Picture, CircleCheck } from '@element-plus/icons-vue'
import { listBuyerOrdersApi, listSellerOrdersApi, cancelOrderApi, payOrderApi, receiveOrderApi, refundOrderApi, shipOrderApi, agreeRefundOrderApi, countBuyerOrdersByStatusApi, countSellerOrdersByStatusApi } from '@/api/order-api'
import { checkReviewedApi, publicReviewListApi, replyReviewApi } from '@/api/review-api'
import type { OrderVO, OrderQueryDTO } from '@/types/order'
import type { ProductReviewVO } from '@/types/review'

// ====== 状态 ======
const router = useRouter()
const loading = ref(false)
const activeRole = ref<'buyer' | 'seller'>('buyer')
const activeStatus = ref<number | undefined>(undefined)
const activeStatusStr = ref('')
const keyword = ref('')
const orderList = ref<OrderVO[]>([])
const totalCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(5)

// 各状态订单数量统计（key=状态值，value=数量）
const statusCounts = ref<Record<number, number>>({})

// 评价状态映射：orderId -> boolean（是否已评价）
const reviewedMap = reactive<Record<number, boolean>>({})

// 卖家评价弹窗
const dialogReviewVisible = ref(false)
const reviewDialogLoading = ref(false)
const reviewDialogList = ref<ProductReviewVO[]>([])
const reviewDialogProductTitle = ref('')
const replyTextMap = reactive<Record<number, string>>({})
const replyLoadingId = ref<number | null>(null)
const currentSellerProductId = ref<number>(0)

// 支付弹窗
const dialogPayVisible = ref(false)
const payLoading = ref(false)
const selectedPayMethod = ref<number>(1)
const currentOrderId = ref<number>(0)

const payMethods = [
    { value: 1, label: '微信支付', icon: '💳' },
    { value: 2, label: '支付宝', icon: '🔵' },
    { value: 3, label: '线下交易', icon: '🤝' }
]

// 角色标签配置（胶囊）
const roleTabs = [
    { label: '我的购买', value: 'buyer' },
    { label: '我的出售', value: 'seller' }
]

// 状态标签配置（el-tabs）
const statusTabs = [
    { label: '全部', value: undefined, name: '' },
    { label: '待付款', value: 0, name: '0' },
    { label: '待发货', value: 1, name: '1' },
    { label: '待收货', value: 2, name: '2' },
    { label: '已完成', value: 3, name: '3' },
    { label: '已取消', value: 4, name: '4' },
    { label: '退款/售后', value: 5, name: '5' }
]

const emptyText = computed(() => '暂无相关订单')

// 带数量的状态标签（用于 el-tabs）
const statusTabsWithCount = computed(() =>
    statusTabs.map(tab => {
        let label = tab.label
        // 全部标签显示总数量 = 所有状态数量之和
        if (tab.value === undefined) {
            const total = Object.values(statusCounts.value).reduce((sum, c) => sum + c, 0)
            if (total > 0) label += `（${total}）`
        } else {
            const count = statusCounts.value[tab.value] ?? 0
            if (count > 0) label += `（${count}）`
        }
        return { ...tab, label }
    })
)

// ====== 工具函数 ======
const getImageUrl = (path?: string) => {
    if (!path) return ''
    if (path.startsWith('http://') || path.startsWith('https://')) return path
    return `${import.meta.env.VITE_API_BASE_URL}${path}`
}

const statusLabel = (v?: number) => {
    const map: Record<number, string> = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '退款/售后' }
    return v !== undefined && v !== null ? map[v] || '未知' : '未知'
}

const statusTagType = (v?: number) => {
    const map: Record<number, string> = { 0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 5: 'danger' }
    return v !== undefined && v !== null ? map[v] || 'info' : 'info'
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
// ====== 事件 ======
const handleRoleSwitch = (role: 'buyer' | 'seller') => {
    activeRole.value = role
    currentPage.value = 1
    activeStatus.value = undefined
    activeStatusStr.value = ''
    keyword.value = ''
    loadOrders()
}

const onStatusChange = (name: string | number) => {
    activeStatus.value = name !== '' ? Number(name) : undefined
    currentPage.value = 1
    loadOrders()
}

// 角色切换时自动重置状态字符串
watch(activeRole, () => {
    activeStatusStr.value = ''
})

const doSearch = () => {
    currentPage.value = 1
    loadOrders()
}

// ====== 检查订单评价状态 ======
const checkReviewStatus = async () => {
    // 只检查已完成订单的评价状态
    const completedOrders = orderList.value.filter(o => o.orderStatus === 3 && o.id)
    if (completedOrders.length === 0) return
    try {
        const results = await Promise.all(
            completedOrders.map(o => checkReviewedApi(o.id!).then(r => ({ id: o.id!, reviewed: r?.data?.reviewed ?? false })))
        )
        results.forEach(r => {
            reviewedMap[r.id] = r.reviewed
        })
    } catch {
        // 静默失败，不影响列表展示
    }
}

// ====== 加载各状态订单数量统计 ======
const loadStatusCounts = async () => {
    try {
        if (activeRole.value === 'buyer') {
            const res = await countBuyerOrdersByStatusApi()
            statusCounts.value = res?.data ?? {}
        } else {
            const res = await countSellerOrdersByStatusApi()
            statusCounts.value = res?.data ?? {}
        }
    } catch {
        // 静默失败，不影响列表展示
    }
}

// ====== 加载订单 ======
const loadOrders = async () => {
    loading.value = true
    try {
        const params: OrderQueryDTO = {
            current: currentPage.value,
            size: pageSize.value,
            orderStatus: activeStatus.value,
            keyword: keyword.value || undefined
        }
        let res
        if (activeRole.value === 'buyer') {
            res = await listBuyerOrdersApi(params)
        } else {
            res = await listSellerOrdersApi(params)
        }
        orderList.value = res?.data ?? []
        totalCount.value = res?.count ?? 0
        // 加载完成后检查已完成订单的评价状态
        checkReviewStatus()
    } catch (e: any) {
        ElMessage.error(e.message || '加载订单失败')
    } finally {
        loading.value = false
    }
    // 始终同步刷新各状态数量统计（即使加载失败）
    loadStatusCounts()
}

// ====== 买家操作 ======
const handlePay = (order: OrderVO) => {
    currentOrderId.value = order.id!
    selectedPayMethod.value = 1
    dialogPayVisible.value = true
}

const confirmPay = async () => {
    if (!currentOrderId.value) return
    payLoading.value = true
    try {
        await payOrderApi(currentOrderId.value, selectedPayMethod.value)
        ElMessage.success('支付成功！')
        dialogPayVisible.value = false
        loadOrders()
    } catch (e: any) {
        ElMessage.error(e.message || '支付失败')
    } finally {
        payLoading.value = false
    }
}

const handleCancel = async (order: OrderVO) => {
    try {
        await ElMessageBox.confirm('确定要取消该订单吗？', '取消订单', {
            confirmButtonText: '确定取消',
            cancelButtonText: '再想想',
            type: 'warning'
        })
        await cancelOrderApi(order.id!)
        ElMessage.success('订单已取消')
        loadOrders()
    } catch {
        // 用户取消操作
    }
}

const handleReceive = async (order: OrderVO) => {
    try {
        await ElMessageBox.confirm('请确认已收到商品，确定收货吗？', '确认收货', {
            confirmButtonText: '确定收货',
            cancelButtonText: '再等等',
            type: 'info'
        })
        await receiveOrderApi(order.id!)
        ElMessage.success('确认收货成功！')
        loadOrders()
    } catch {
        // 用户取消操作
    }
}

const handleReview = (order: OrderVO) => {
    router.push(`/user-home/add-review/${order.id}`)
}

const handleRefund = async (order: OrderVO) => {
    try {
        await ElMessageBox.confirm('确定要申请退款/售后吗？', '退款申请', {
            confirmButtonText: '确认申请',
            cancelButtonText: '取消',
            type: 'warning'
        })
        await refundOrderApi(order.id!)
        ElMessage.success('退款申请已提交，请等待卖家处理')
        loadOrders()
    } catch {
        // 用户取消操作
    }
}

// ====== 卖家操作 - 查看/回复评价 ======
const handleViewReview = async (order: OrderVO) => {
    if (!order.productId) return
    currentSellerProductId.value = order.productId
    reviewDialogProductTitle.value = order.productTitle || ''
    reviewDialogList.value = []
    dialogReviewVisible.value = true
    reviewDialogLoading.value = true
    try {
        const res = await publicReviewListApi({ productId: order.productId, current: 1, size: 50 })
        reviewDialogList.value = res?.data ?? []
        // 初始化回复文本
        reviewDialogList.value.forEach(r => {
            if (r.id && !r.replyContent) {
                replyTextMap[r.id] = ''
            }
        })
    } catch {
        // 忽略
    } finally {
        reviewDialogLoading.value = false
    }
}

const handleReply = async (review: ProductReviewVO) => {
    if (!review.id || !replyTextMap[review.id]?.trim()) return
    replyLoadingId.value = review.id
    try {
        await replyReviewApi({ id: review.id, replyContent: replyTextMap[review.id].trim() })
        ElMessage.success('回复成功')
        // 刷新
        handleViewReview({ productId: review.productId, productTitle: reviewDialogProductTitle } as any)
    } catch (e: any) {
        ElMessage.error(e.message || '回复失败')
    } finally {
        replyLoadingId.value = null
    }
}

// ====== 卖家操作 - 发货/退款 ======
const handleShip = async (order: OrderVO) => {
    try {
        await ElMessageBox.confirm('确定已发货吗？', '确认发货', {
            confirmButtonText: '确认发货',
            cancelButtonText: '取消',
            type: 'info'
        })
        await shipOrderApi(order.id!)
        ElMessage.success('已标记为发货！')
        loadOrders()
    } catch {
        // 用户取消操作
    }
}

const handleRefundAgree = async (order: OrderVO) => {
    try {
        await ElMessageBox.confirm('确定同意退款吗？', '同意退款', {
            confirmButtonText: '同意退款',
            cancelButtonText: '拒绝',
            type: 'warning'
        })
        await agreeRefundOrderApi(order.id!)
        ElMessage.success('已同意退款，订单已取消')
        loadOrders()
    } catch {
        // 用户取消操作
    }
}

onMounted(() => {
    loadOrders()
})
</script>

<style scoped lang="scss">
// =====================================================
// 📦 我的订单 — 与我的商品风格统一
// =====================================================

.user-orders {
    // ====== 基础布局 ======
    margin: 0 auto;
    padding-bottom: 80px;

    // ====== 角色切换（胶囊风格） ======
    .role-tabs {
        display: flex;
        justify-content: center;
        margin-bottom: 16px;

        .tab-panel {
            display: inline-flex;
            background-color: rgb(240, 240, 240);
            border-radius: 30px;
            padding: 5px;
            align-items: center;

            .tab-item {
                display: inline-flex;
                align-items: center;
                gap: 5px;
                padding: 6px 22px;
                border-radius: 20px;
                font-size: 14px;
                font-weight: 500;
                color: #666;
                cursor: pointer;
                transition: all 0.2s;
                user-select: none;

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

    // ====== 状态筛选（el-tabs 白色卡片风格） ======
    .status-tabs {
        background: #fff;
        border-radius: 14px;
        padding: 0 20px;
        margin-bottom: 16px;

        // 去掉 el-tabs 默认的内边距/边框/背景
        :deep(.el-tabs__header) {
            margin: 0;
            border-bottom: none;
        }

        :deep(.el-tabs__nav-wrap) {
            &::after {
                display: none;
            }
        }

        :deep(.el-tabs__item) {
            height: 48px;
            line-height: 48px;
            font-size: 15px;
            font-weight: 500;
            color: #636366;
            padding: 0 10px;
            margin-right: 28px;
            transition: color 0.2s;

            &:hover {
                color: #1a1a1a;
            }

            &.is-active {
                color: #1a1a1a;
                font-weight: 600;
            }
        }

        // 激活指示条 — 深色
        :deep(.el-tabs__active-bar) {
            height: 3px;
            background: #1a1a1a;
            border-radius: 2px;
        }
    }

    // ====== 搜索栏（统一首页导航搜索框风格） ======
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

    // ====== 空状态 ======
    .empty-state {
        padding: 60px 0;
        background: #fff;
        border-radius: 14px;
        box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
    }

    // ====== 订单卡片（白灰黑简约风格） ======
    .order-card {
        background: #fff;
        border-radius: 10px;
        margin: 14px 20px;
        overflow: hidden;
    }

    // 订单头部（编号）
    .order-header {
        padding: 12px 18px;
        background: #fafafa;

        .order-no {
            font-size: 13px;
            color: #999;

            .label { color: #bbb; }
            .value {
                font-family: 'Courier New', monospace;
                color: #333;
            }
        }
    }

    // 订单主体（商品信息 + 操作）
    .order-body {
        display: flex;
        gap: 14px;
        padding: 14px 18px;
        align-items: center;

        // 商品图片外层（用于状态悬浮定位）
        .product-img-wrapper {
            position: relative;
            flex-shrink: 0;

            .product-img {
                width: 60px;
                height: 60px;
                border-radius: 6px;
                display: block;
                background: #f5f5f5;
            }

            .img-placeholder {
                display: flex;
                align-items: center;
                justify-content: center;
                width: 100%;
                height: 100%;
                color: #ccc;
                background: #f5f5f5;
                border-radius: 8px;
            }

            // 状态标签固定在封面左上角
            .status-overlay {
                position: absolute;
                top: 0;
                left: 0;
                border-radius: 6px 0 6px 0;
                font-size: 11px;
                height: 20px;
                line-height: 20px;
                padding: 0 8px;
            }

            // 创建时间标签在商品图下方
            .create-time-tag {
                display: block;
                width: 100%;
                margin-top: 6px;
                text-align: center;
                font-size: 11px;
                border: none;
                background: transparent;
                color: #999;
                padding: 0;
            }
        }

        .product-info {
            flex: 1;
            display: flex;
            flex-direction: column;
            gap: 6px;
            min-width: 0;

            .product-title {
                font-size: 15px;
                font-weight: 600;
                color: #222;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }

            .product-meta {
                display: flex;
                gap: 10px;
                align-items: center;
                .price { font-size: 16px; color: #222; font-weight: 700; }
                .quantity { font-size: 13px; color: #999; }
            }

            .trade-partner {
                .el-tag {
                    height: 20px;
                    line-height: 20px;
                    padding: 0 6px;
                    font-size: 11px;
                    border: none;
                    background: #f0f0f0;
                    color: #999;
                }
            }
        }

    }

    // 右侧：金额 + 操作按钮
    .order-right {
        flex-shrink: 0;
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        justify-content: center;
        gap: 12px;

        .order-amount {
            text-align: right;
            .amount {
                font-size: 18px;
                font-weight: 700;
                color: #222;
            }
            .fee {
                font-size: 12px;
                color: #bbb;
                margin-top: 2px;
            }
        }

        .order-actions {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            justify-content: flex-end;

            :deep(.el-button) {
                border-radius: 6px;
                font-size: 13px;
                padding: 10px 16px;
                height: auto;
                line-height: 1.4;

                &.is-disabled {
                    background: #f5f5f5;
                    border-color: #eee;
                    color: #bbb;
                    opacity: 1;
                }
            }

            :deep(.el-button--primary) {
                background: #222;
                border-color: #222;
                color: #fff;
                &:hover { background: #444; border-color: #444; }
                &.is-disabled { background: #f5f5f5; border-color: #eee; color: #bbb; }
            }

            :deep(.el-button--success) {
                background: #222;
                border-color: #222;
                color: #fff;
                &:hover { background: #444; border-color: #444; }
            }

            :deep(.el-button--warning) {
                background: #fff;
                border-color: #ccc;
                color: #333;
                &:hover { border-color: #999; }
            }
        }
    }

    // 收货地址
    .address-info {
        padding: 10px 18px;
        background: #fafafa;
        font-size: 13px;
        color: #666;
        line-height: 1.5;
        .address-text { color: #999; }
    }

    // 买家备注
    .remark-info {
        padding: 10px 18px;
        font-size: 13px;
        color: #666;
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

    // ====== 卖家评价弹窗 ======
    .review-dialog-body {
        min-height: 100px;
    }

    .review-empty {
        padding: 20px 0;
    }

    .review-dialog-item {
        padding: 16px 0;
        border-bottom: 1px solid #f0f0f4;

        &:last-child {
            border-bottom: none;
        }

        .dialog-review-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 10px;

            .dialog-review-buyer {
                font-size: 14px;
                font-weight: 500;
                color: #1a1a1a;
            }
        }

        .dialog-review-content {
            font-size: 14px;
            color: #636366;
            line-height: 1.6;
            margin-bottom: 10px;
            white-space: pre-wrap;
            word-break: break-word;
        }

        .dialog-review-images {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-bottom: 10px;

            .dialog-review-img {
                width: 60px;
                height: 60px;
                border-radius: 4px;
                object-fit: cover;
                border: 1px solid #f0f0f4;
            }
        }

        .dialog-seller-reply {
            background: #f8f8fa;
            border-radius: 8px;
            padding: 10px 14px;
            font-size: 13px;
            color: #636366;
            line-height: 1.6;

            .reply-label {
                font-weight: 600;
                color: #1a1a1a;
            }
        }

        .dialog-reply-editor {
            background: #f8f8fa;
            border-radius: 8px;
            padding: 12px;
        }
    }

    // ====== 支付方式弹窗 ======
    .pay-methods {
        display: flex;
        flex-direction: column;
        gap: 12px;
    }

    .pay-method-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 14px 16px;
        border: 1px solid #e5e5ea;
        border-radius: 10px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
            border-color: #1a1a1a;
        }

        &.selected {
            border-color: #1a1a1a;
            background: rgba(26, 26, 26, 0.03);
        }

        .pay-icon {
            font-size: 24px;
        }

        .pay-name {
            flex: 1;
            font-size: 14px;
            font-weight: 500;
        }
    }
}
</style>
