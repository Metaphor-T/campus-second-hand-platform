<template>
    <div class="order-management">
        <!-- 搜索表单 -->
        <el-card class="search-card" shadow="never">
            <el-form :model="queryDTO" inline>
                <el-form-item label="订单状态">
                    <el-select v-model="queryDTO.orderStatus" placeholder="全部" clearable style="width: 140px;">
                        <el-option label="待付款" :value="0" />
                        <el-option label="待发货" :value="1" />
                        <el-option label="待收货" :value="2" />
                        <el-option label="已完成" :value="3" />
                        <el-option label="已取消" :value="4" />
                        <el-option label="退款/售后" :value="5" />
                    </el-select>
                </el-form-item>
                <el-form-item label="关键词">
                    <el-input v-model="queryDTO.keyword" placeholder="订单号/商品名/用户名" clearable style="width: 200px;" />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
                    <el-button @click="handleReset" :icon="RefreshLeft">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 数据表格 -->
        <el-card class="table-card" shadow="never">
            <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
                <el-table-column prop="id" label="ID" width="60" align="center" />
                <el-table-column prop="orderNo" label="订单号" width="190" show-overflow-tooltip />
                <el-table-column prop="productTitle" label="商品" min-width="140" show-overflow-tooltip />
                <el-table-column prop="buyerName" label="买家" width="90" align="center" />
                <el-table-column prop="sellerName" label="卖家" width="90" align="center" />
                <el-table-column prop="payAmount" label="实付" width="100" align="center">
                    <template #default="{ row }">
                        <span class="price-text">¥{{ row.payAmount }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="orderStatus" label="状态" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag :type="statusTagType(row.orderStatus)" effect="plain" size="small">
                            {{ statusLabel(row.orderStatus) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="下单时间" width="160" align="center" />
                <el-table-column label="操作" width="200" align="center" fixed="right">
                    <template #default="{ row }">
                        <div class="action-btns">
                            <el-button type="primary" size="small" @click="handleDetail(row)" :icon="View">详情</el-button>
                            <el-button
                                v-if="row.orderStatus === 5"
                                type="success"
                                size="small"
                                @click="handleAgreeRefund(row)"
                                :icon="Select"
                            >
                                同意退款
                            </el-button>
                        </div>
                    </template>
                </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="pagination-wrapper">
                <el-pagination
                    v-model:current-page="queryDTO.current"
                    v-model:page-size="queryDTO.size"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="total"
                    layout="total, sizes, prev, pager, next, jumper"
                    background
                    @size-change="fetchData"
                    @current-change="fetchData"
                />
            </div>
        </el-card>

        <!-- 详情弹窗 -->
        <el-dialog v-model="detailVisible" title="订单详情" width="640px" destroy-on-close>
            <div class="detail-body" v-if="detailRow">
                <div class="detail-section">
                    <h4>基本信息</h4>
                    <div class="detail-field"><span class="dl">订单号：</span><span class="dv">{{ detailRow.orderNo }}</span></div>
                    <div class="detail-field"><span class="dl">商品：</span><span class="dv">{{ detailRow.productTitle }}</span></div>
                    <div class="detail-field"><span class="dl">买家：</span><span class="dv">{{ detailRow.buyerName }}</span></div>
                    <div class="detail-field"><span class="dl">卖家：</span><span class="dv">{{ detailRow.sellerName }}</span></div>
                    <div class="detail-field"><span class="dl">数量：</span><span class="dv">{{ detailRow.quantity }}</span></div>
                    <div class="detail-field"><span class="dl">单价：</span><span class="dv">¥{{ detailRow.unitPrice }}</span></div>
                    <div class="detail-field"><span class="dl">状态：</span>
                        <el-tag :type="statusTagType(detailRow.orderStatus)" effect="plain" size="small">
                            {{ statusLabel(detailRow.orderStatus) }}
                        </el-tag>
                    </div>
                </div>
                <div class="detail-section">
                    <h4>费用信息</h4>
                    <div class="detail-field"><span class="dl">商品总额：</span><span class="dv">¥{{ detailRow.totalAmount }}</span></div>
                    <div class="detail-field"><span class="dl">运费：</span><span class="dv">¥{{ detailRow.shippingFee }}</span></div>
                    <div class="detail-field"><span class="dl price">实付金额：</span><span class="dv price">¥{{ detailRow.payAmount }}</span></div>
                </div>
                <div v-if="detailRow.fullAddress" class="detail-section">
                    <h4>收货信息</h4>
                    <div class="detail-field"><span class="dl">收件人：</span><span class="dv">{{ detailRow.receiverName }}</span></div>
                    <div class="detail-field"><span class="dl">电话：</span><span class="dv">{{ detailRow.receiverPhone }}</span></div>
                    <div class="detail-field"><span class="dl">地址：</span><span class="dv">{{ detailRow.fullAddress }}</span></div>
                </div>
                <div class="detail-section">
                    <h4>时间线</h4>
                    <div class="detail-field"><span class="dl">下单时间：</span><span class="dv">{{ detailRow.createTime }}</span></div>
                    <div class="detail-field" v-if="detailRow.payTime"><span class="dl">支付时间：</span><span class="dv">{{ detailRow.payTime }}</span></div>
                    <div class="detail-field" v-if="detailRow.shipTime"><span class="dl">发货时间：</span><span class="dv">{{ detailRow.shipTime }}</span></div>
                    <div class="detail-field" v-if="detailRow.receiveTime"><span class="dl">收货时间：</span><span class="dv">{{ detailRow.receiveTime }}</span></div>
                    <div class="detail-field" v-if="detailRow.cancelTime"><span class="dl">取消时间：</span><span class="dv">{{ detailRow.cancelTime }}</span></div>
                    <div class="detail-field" v-if="detailRow.cancelReason"><span class="dl">取消原因：</span><span class="dv remark">{{ detailRow.cancelReason }}</span></div>
                </div>
                <div class="detail-section" v-if="detailRow.buyerRemark">
                    <h4>买家备注</h4>
                    <div class="detail-field"><span class="dv">{{ detailRow.buyerRemark }}</span></div>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, View, Select } from '@element-plus/icons-vue'
import { listAllOrdersApi, adminAgreeRefundApi } from '@/api/order-api'
import type { OrderVO, OrderQueryDTO } from '@/types/order'

const loading = ref(false)
const tableData = ref<OrderVO[]>([])
const total = ref(0)

const queryDTO = reactive<OrderQueryDTO>({
    current: 1,
    size: 10,
    orderStatus: undefined,
    keyword: ''
})

// 详情
const detailVisible = ref(false)
const detailRow = ref<OrderVO | null>(null)

const statusLabel = (v?: number) =>
    ['待付款', '待发货', '待收货', '已完成', '已取消', '退款/售后'][v ?? 0] || '-'

const statusTagType = (v?: number) =>
    ['warning', '', 'primary', 'success', 'info', 'danger'][v ?? 0] as any

// 加载数据
const fetchData = async () => {
    try {
        loading.value = true
        const res = await listAllOrdersApi(queryDTO)
        tableData.value = res?.data ?? []
        total.value = res?.count ?? 0
    } catch (e: any) {
        ElMessage.error(e.message || '获取数据失败')
    } finally {
        loading.value = false
    }
}

const handleSearch = () => { queryDTO.current = 1; fetchData() }
const handleReset = () => {
    queryDTO.orderStatus = undefined
    queryDTO.keyword = ''
    queryDTO.current = 1
    fetchData()
}

// 详情
const handleDetail = (row: OrderVO) => {
    detailRow.value = { ...row }
    detailVisible.value = true
}

// 同意退款
const handleAgreeRefund = (row: OrderVO) => {
    ElMessageBox.confirm(
        `确定同意订单 "${row.orderNo}" 的退款申请吗？`,
        '退款审核确认',
        {
            confirmButtonText: '同意退款',
            cancelButtonText: '取消',
            type: 'warning'
        }
    ).then(async () => {
        try {
            loading.value = true
            await adminAgreeRefundApi(row.id!)
            ElMessage.success('已同意退款')
            fetchData()
        } catch (e: any) {
            ElMessage.error(e.message || '操作失败')
        } finally {
            loading.value = false
        }
    }).catch(() => {})
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.order-management {
    .search-card {
        margin-bottom: 20px;
        :deep(.el-card__body) { padding-bottom: 0; }
        .el-form--inline .el-form-item { margin-bottom: 16px; }
    }
    .table-card {
        :deep(.el-card__body) { padding: 20px; }
    }
    .pagination-wrapper {
        display: flex; justify-content: flex-end; margin-top: 20px;
    }
    .price-text { color: #e6a23c; font-weight: 600; font-size: 14px; }
    .dialog-footer { text-align: right; }
    .action-btns {
        display: flex;
        gap: 6px;
        justify-content: center;
        padding: 8px 0;

        .el-button {
            padding: 8px 14px;
            height: auto;
            line-height: 1.4;
        }
    }

    .detail-body {
        .detail-section {
            margin-bottom: 24px;
            h4 {
                font-size: 16px;
                font-weight: 600;
                color: #303133;
                margin-bottom: 14px;
                padding-bottom: 8px;
                border-bottom: 2px solid #ebeef5;
            }
        }
        .detail-field {
            display: flex;
            align-items: flex-start;
            margin-bottom: 12px;
            font-size: 15px;
            .dl {
                color: #909399;
                flex-shrink: 0;
                width: 90px;
                text-align: right;
            }
            .dv { color: #303133; }
            .price { color: #e6a23c; font-weight: 600; }
            .remark { color: #f56c6c; }
        }
    }
}
</style>
