<template>
    <div class="product-management">
        <!-- 搜索表单 -->
        <el-card class="search-card" shadow="never">
            <el-form :model="queryDTO" inline>
                <el-form-item label="商品标题">
                    <el-input v-model="queryDTO.title" placeholder="请输入标题" clearable style="width: 160px;" />
                </el-form-item>
                <el-form-item label="审核状态">
                    <el-select v-model="queryDTO.status" placeholder="全部" clearable style="width: 120px;">
                        <el-option label="待审核" :value="0" />
                        <el-option label="审核通过" :value="1" />
                        <el-option label="审核拒绝" :value="2" />
                        <el-option label="已下架" :value="3" />
                    </el-select>
                </el-form-item>
                <el-form-item label="新旧程度">
                    <el-select v-model="queryDTO.condition" placeholder="全部" clearable style="width: 120px;">
                        <el-option label="全新" :value="1" />
                        <el-option label="几乎全新" :value="2" />
                        <el-option label="有使用痕迹" :value="3" />
                        <el-option label="老旧" :value="4" />
                    </el-select>
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
                <el-table-column prop="title" label="商品标题" min-width="180" show-overflow-tooltip />
                <el-table-column prop="username" label="发布者" width="100" align="center" />
                <el-table-column prop="categoryName" label="分类" width="100" align="center" />
                <el-table-column prop="price" label="价格" width="100" align="center">
                    <template #default="{ row }">
                        <span class="price-text">¥{{ row.price }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="condition" label="新旧" width="100" align="center">
                    <template #default="{ row }">
                        {{ conditionLabel(row.condition) }}
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag :type="statusTagType(row.status)" effect="plain" size="small">
                            {{ statusLabel(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="发布时间" min-width="160" align="center" />
                <el-table-column label="操作" width="200" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button type="info" link size="small" @click="handleDetail(row)" :icon="View">详情</el-button>
                        <el-button v-if="row.status === 0" type="success" link size="small" @click="handleAudit(row, 1)" :icon="Select">通过</el-button>
                        <el-button v-if="row.status === 0" type="warning" link size="small" @click="handleAudit(row, 2)" :icon="CloseBold">拒绝</el-button>
                        <el-button type="danger" link size="small" @click="handleDelete(row)" :icon="Delete">删除</el-button>
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
        <el-dialog v-model="detailVisible" title="商品详情" width="560px" destroy-on-close>
            <div class="detail-body" v-if="detailRow">
                <div v-if="detailRow.images && detailRow.images.length > 0" class="detail-images">
                    <el-image
                        v-for="(img, idx) in detailRow.images"
                        :key="idx"
                        :src="getImageUrl(img.imageUrl)"
                        :preview-src-list="detailRow.images.map((i: any) => getImageUrl(i.imageUrl))"
                        fit="cover"
                        class="detail-image"
                    />
                </div>
                <div class="detail-field"><span class="dl">标题：</span><span class="dv">{{ detailRow.title }}</span></div>
                <div class="detail-field"><span class="dl">发布者：</span><span class="dv">{{ detailRow.username }}</span></div>
                <div class="detail-field"><span class="dl">分类：</span><span class="dv">{{ detailRow.categoryName }}</span></div>
                <div class="detail-field"><span class="dl">价格：</span><span class="dv price">¥{{ detailRow.price }}</span>
                    <template v-if="detailRow.originalPrice">
                        <span class="original-price">¥{{ detailRow.originalPrice }}</span>
                    </template>
                </div>
                <div class="detail-field"><span class="dl">新旧程度：</span><span class="dv">{{ conditionLabel(detailRow.condition) }}</span></div>
                <div class="detail-field"><span class="dl">交易方式：</span><span class="dv">{{ tradeTypeLabel(detailRow.tradeType) }}</span></div>
                <div class="detail-field"><span class="dl">描述：</span><span class="dv">{{ detailRow.description || '暂无描述' }}</span></div>
                <div v-if="detailRow.auditRemark" class="detail-field"><span class="dl">审核备注：</span><span class="dv remark">{{ detailRow.auditRemark }}</span></div>
            </div>
        </el-dialog>

        <!-- 审核拒绝弹窗 -->
        <el-dialog v-model="rejectVisible" title="审核拒绝" width="400px" destroy-on-close>
            <el-input v-model="rejectRemark" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="rejectVisible = false">取消</el-button>
                    <el-button type="warning" :loading="auditLoading" @click="submitReject">确认拒绝</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, View, Select, CloseBold, Delete } from '@element-plus/icons-vue'
import { productListPageApi, auditProductApi, deleteProductByIdApi } from '@/api/product-api'
import type { ProductVO, ProductQueryDTO } from '@/types/product'

const loading = ref(false)
const auditLoading = ref(false)
const tableData = ref<ProductVO[]>([])
const total = ref(0)

const queryDTO = reactive<ProductQueryDTO>({
    current: 1,
    size: 10,
    title: '',
    status: undefined,
    condition: undefined
})

// 详情
const detailVisible = ref(false)
const detailRow = ref<ProductVO | null>(null)

// 拒绝
const rejectVisible = ref(false)
const rejectRemark = ref('')
const rejectProductId = ref<number | undefined>(undefined)

const getImageUrl = (path?: string) => {
    if (!path) return ''
    if (path.startsWith('http://') || path.startsWith('https://')) return path
    return `${import.meta.env.VITE_API_BASE_URL}${path}`
}

const conditionLabel = (v?: number) => ['', '全新', '几乎全新', '有使用痕迹', '老旧'][v ?? 0] || '-'
const tradeTypeLabel = (v?: number) => ['', '自提', '邮寄', '两者皆可'][v ?? 0] || '-'
const statusLabel = (v?: number) => ['待审核', '审核通过', '审核拒绝', '已下架'][v ?? 0] || '-'
const statusTagType = (v?: number) => ['warning', 'success', 'danger', 'info'][v ?? 0] as any

// 加载数据
const fetchData = async () => {
    try {
        loading.value = true
        const res = await productListPageApi(queryDTO)
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
    queryDTO.title = ''
    queryDTO.status = undefined
    queryDTO.condition = undefined
    queryDTO.current = 1
    fetchData()
}

// 详情
const handleDetail = (row: ProductVO) => {
    detailRow.value = { ...row }
    detailVisible.value = true
}

// 审核
const handleAudit = (row: ProductVO, status: number) => {
    if (status === 1) {
        // 通过
        ElMessageBox.confirm(`确定审核通过 "${row.title}" 吗？`, '审核确认', {
            confirmButtonText: '通过', cancelButtonText: '取消', type: 'success'
        }).then(async () => {
            try {
                auditLoading.value = true
                await auditProductApi({ id: row.id!, status: 1 })
                ElMessage.success('已审核通过')
                fetchData()
            } catch (e: any) {
                ElMessage.error(e.message || '审核失败')
            } finally {
                auditLoading.value = false
            }
        }).catch(() => {})
    } else {
        // 拒绝
        rejectProductId.value = row.id
        rejectRemark.value = ''
        rejectVisible.value = true
    }
}

const submitReject = async () => {
    try {
        auditLoading.value = true
        await auditProductApi({
            id: rejectProductId.value!,
            status: 2,
            auditRemark: rejectRemark.value
        })
        ElMessage.success('已拒绝')
        rejectVisible.value = false
        fetchData()
    } catch (e: any) {
        ElMessage.error(e.message || '操作失败')
    } finally {
        auditLoading.value = false
    }
}

// 删除
const handleDelete = (row: ProductVO) => {
    ElMessageBox.confirm(`确定删除 "${row.title}" 吗？`, '删除确认', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    }).then(async () => {
        try {
            await deleteProductByIdApi(row.id!)
            ElMessage.success('删除成功')
            fetchData()
        } catch (e: any) {
            ElMessage.error(e.message || '删除失败')
        }
    }).catch(() => {})
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.product-management {
    .search-card {
        margin-bottom: 16px;
        :deep(.el-card__body) { padding-bottom: 0; }
        .el-form--inline .el-form-item { margin-bottom: 16px; }
    }
    .table-card {
        :deep(.el-card__body) { padding: 16px; }
    }
    .pagination-wrapper {
        display: flex; justify-content: flex-end; margin-top: 16px;
    }
    .price-text { color: #e6a23c; font-weight: 500; }
    .dialog-footer { text-align: right; }

    .detail-body {
        .detail-images {
            display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px;
            .detail-image {
                width: 100px; height: 100px; border-radius: 6px; cursor: pointer;
                border: 1px solid #e8ecf1;
            }
        }
        .detail-field {
            display: flex; align-items: flex-start; margin-bottom: 10px; font-size: 14px;
            .dl { color: #909399; flex-shrink: 0; width: 80px; text-align: right; }
            .dv { color: #303133; }
            .price { color: #e6a23c; font-weight: 600; }
            .original-price { text-decoration: line-through; color: #c0c4cc; margin-left: 8px; font-size: 13px; }
            .remark { color: #f56c6c; }
        }
    }
}
</style>
