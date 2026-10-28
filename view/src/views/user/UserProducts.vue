<template>
    <div class="my-products">
        <!-- ====== 搜索区 ====== -->
        <div class="search-section">
            <el-form :model="queryDTO" inline>
                <el-form-item label="标题">
                    <el-input v-model="queryDTO.title" placeholder="搜索我的商品" clearable style="width: 200px;" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="queryDTO.status" placeholder="全部" clearable style="width: 120px;">
                        <el-option label="待审核" :value="0" />
                        <el-option label="审核通过" :value="1" />
                        <el-option label="审核拒绝" :value="2" />
                        <el-option label="已下架" :value="3" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
                    <el-button @click="handleReset" :icon="RefreshLeft">重置</el-button>
                    <el-button type="primary" @click="handleAdd" :icon="Plus" class="page-action-btn">发布商品</el-button>
                </el-form-item>
            </el-form>
        </div>

        <!-- ====== 商品列表表格 ====== -->
        <div class="table-card">
            <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
                <el-table-column label="封面" width="80" align="center">
                    <template #default="{ row }">
                        <el-image
                            v-if="coverImage(row)"
                            :src="getImageUrl(coverImage(row))"
                            fit="cover"
                            style="width: 50px; height: 50px; border-radius: 4px;"
                            preview-teleported
                        />
                        <span v-else class="no-cover">无图</span>
                    </template>
                </el-table-column>
                <el-table-column prop="title" label="商品标题" min-width="180" show-overflow-tooltip />
                <el-table-column prop="categoryName" label="分类" width="100" align="center" />
                <el-table-column prop="price" label="价格" width="100" align="center">
                    <template #default="{ row }">
                        <span class="price-text">¥{{ row.price }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="statusType(row.status)" effect="plain" size="small">
                            {{ statusLabel(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="发布时间" min-width="150" align="center" />
                <el-table-column label="操作" width="220" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" link size="small" @click="handleEdit(row)" :icon="EditPen">编辑</el-button>
                        <el-button v-if="row.status !== 3" type="warning" link size="small" @click="handleDown(row)" :icon="Bottom">下架</el-button>
                        <el-button v-if="row.status === 3" type="success" link size="small" @click="handleUp(row)" :icon="Top">上架</el-button>
                        <el-button type="danger" link size="small" @click="handleDelete(row)" :icon="Delete">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-wrapper">
                <el-pagination
                    v-model:current-page="queryDTO.current"
                    v-model:page-size="queryDTO.size"
                    :page-sizes="[10, 20, 50]"
                    :total="total"
                    layout="total, sizes, prev, pager, next, jumper"
                    background
                    @size-change="fetchData"
                    @current-change="fetchData"
                />
            </div>
        </div>

        <!-- 发布/编辑弹窗 -->
        <el-dialog
            v-model="dialogVisible"
            :title="isEditing ? '编辑商品' : '发布商品'"
            width="680px"
            :before-close="closeDialog"
            destroy-on-close
        >
            <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
                <el-form-item label="商品标题" prop="title">
                    <el-input v-model="formData.title" placeholder="请输入商品标题" maxlength="200" />
                </el-form-item>

                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="分类" prop="categoryId">
                            <el-tree-select
                                v-model="formData.categoryId"
                                :data="categoryTree"
                                :props="{ value: 'id', label: 'name', children: 'children' }"
                                placeholder="选择分类"
                                check-strictly
                                style="width: 100%;"
                            />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="新旧程度" prop="condition">
                            <el-select v-model="formData.condition" placeholder="选择新旧程度" style="width: 100%;">
                                <el-option label="全新" :value="1" />
                                <el-option label="几乎全新" :value="2" />
                                <el-option label="有使用痕迹" :value="3" />
                                <el-option label="老旧" :value="4" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="售价" prop="price">
                            <el-input-number v-model="formData.price" :min="0" :precision="2" style="width: 100%;" placeholder="¥" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="原价">
                            <el-input-number v-model="formData.originalPrice" :min="0" :precision="2" style="width: 100%;" placeholder="选填" />
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-form-item label="交易方式" prop="tradeType">
                    <el-radio-group v-model="formData.tradeType">
                        <el-radio :value="1">自提</el-radio>
                        <el-radio :value="2">邮寄</el-radio>
                        <el-radio :value="3">两者皆可</el-radio>
                    </el-radio-group>
                </el-form-item>

                <el-form-item label="商品描述">
                    <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入商品描述" maxlength="2000" show-word-limit />
                </el-form-item>

                <!-- 图片上传 -->
                <el-form-item label="商品图片">
                    <div class="image-upload-area">
                        <div
                            v-for="(img, idx) in formData.images"
                            :key="idx"
                            class="uploaded-image"
                        >
                            <el-image :src="getImageUrl(img.imageUrl)" fit="cover" class="img-preview" />
                            <div class="img-actions">
                                <el-tag v-if="img.isCover === 1" type="success" size="small" effect="dark">封面</el-tag>
                                <el-button v-else link size="small" type="primary" @click="setCover(idx)">设为封面</el-button>
                                <el-button link size="small" type="danger" @click="removeImage(idx)" :icon="Delete" />
                            </div>
                        </div>

                        <el-upload
                            v-if="formData.images && formData.images.length < 9"
                            :action="uploadUrl"
                            :show-file-list="false"
                            :on-success="onImageUpload"
                            :before-upload="beforeImageUpload"
                            class="upload-trigger"
                        >
                            <el-icon :size="28"><Plus /></el-icon>
                        </el-upload>
                    </div>
                    <p class="upload-tip">最多上传 9 张，第一张默认为封面，支持 JPG/PNG，单张不超过 2MB</p>
                </el-form-item>
            </el-form>

            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="closeDialog">取消</el-button>
                    <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定发布</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, RefreshLeft, Plus, EditPen, Bottom, Delete, Top } from '@element-plus/icons-vue'
import type { UploadProps } from 'element-plus'
import {
    myProductListApi, addProductApi, updateProductApi,
    downProductApi, upProductApi, deleteMyProductApi
} from '@/api/product-api'
import { categoryListAllApi } from '@/api/category-api'
import type { ProductVO, ProductAddDTO, ProductUpdateDTO, ProductImageDTO } from '@/types/product'
import type { CategoryVO } from '@/types/category'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEditing = ref(false)
const tableData = ref<ProductVO[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()
const categoryTree = ref<CategoryVO[]>([])

const queryDTO = reactive({ current: 1, size: 10, title: '', status: undefined as number | undefined })

const uploadUrl = `${import.meta.env.VITE_API_BASE_URL}/files/upload`

const getImageUrl = (path?: string) => {
    if (!path) return ''
    if (path.startsWith('http://') || path.startsWith('https://')) return path
    return `${import.meta.env.VITE_API_BASE_URL}${path}`
}

const statusLabel = (v?: number) => ['待审核', '审核通过', '审核拒绝', '已下架'][v ?? 0] || '-'
const statusType = (v?: number) => ['warning', 'success', 'danger', 'info'][v ?? 0] as any

const coverImage = (row: ProductVO) => {
    const cover = row.images?.find((i: any) => i.isCover === 1)
    return cover?.imageUrl ?? row.images?.[0]?.imageUrl
}

const buildCategoryTree = (flat: CategoryVO[]): CategoryVO[] => {
    const map = new Map<number, CategoryVO>()
    const roots: CategoryVO[] = []
    flat.forEach(item => map.set(item.id!, { ...item, children: [] }))
    flat.forEach(item => {
        const node = map.get(item.id!)!
        if (item.parentId && item.parentId !== 0 && map.has(item.parentId)) {
            map.get(item.parentId)!.children!.push(node)
        } else { roots.push(node) }
    })
    return roots
}

// 表单
const formData = reactive<ProductAddDTO & { id?: number }>({
    id: undefined, categoryId: undefined, title: '', description: '',
    price: undefined, originalPrice: undefined, condition: 1, tradeType: 1, images: []
})

const formRules: FormRules = {
    title: [{ required: true, message: '请输入商品标题', trigger: 'blur' }],
    categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
    price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
    condition: [{ required: true, message: '请选择新旧程度', trigger: 'change' }],
    tradeType: [{ required: true, message: '请选择交易方式', trigger: 'change' }],
}

// 数据加载
const fetchData = async () => {
    try {
        loading.value = true
        const res = await myProductListApi({ current: queryDTO.current, size: queryDTO.size, title: queryDTO.title || undefined, status: queryDTO.status })
        tableData.value = res?.data ?? []
        total.value = res?.count ?? 0
    } catch (e: any) {
        ElMessage.error(e.message || '获取数据失败')
    } finally { loading.value = false }
}

const loadCategories = async () => {
    try {
        const res = await categoryListAllApi()
        categoryTree.value = buildCategoryTree(res?.data ?? [])
    } catch { /* 静默 */ }
}

const handleSearch = () => { queryDTO.current = 1; fetchData() }
const handleReset = () => { queryDTO.title = ''; queryDTO.status = undefined; queryDTO.current = 1; fetchData() }

// 新增
const handleAdd = () => {
    isEditing.value = false
    resetForm()
    dialogVisible.value = true
}

// 编辑
const handleEdit = (row: ProductVO) => {
    isEditing.value = true
    formData.id = row.id
    formData.categoryId = row.categoryId
    formData.title = row.title ?? ''
    formData.description = row.description ?? ''
    formData.price = row.price
    formData.originalPrice = row.originalPrice
    formData.condition = row.condition ?? 1
    formData.tradeType = row.tradeType ?? 1
    formData.images = (row.images ?? []).map((img: any) => ({
        imageUrl: img.imageUrl, isCover: img.isCover ?? 0, sort: img.sort ?? 0
    }))
    dialogVisible.value = true
}

// 下架
const handleDown = (row: ProductVO) => {
    ElMessageBox.confirm(`确定下架 "${row.title}" 吗？`, '下架确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
        .then(async () => {
            try { await downProductApi(row.id!); ElMessage.success('已下架'); fetchData() }
            catch (e: any) { ElMessage.error(e.message || '操作失败') }
        }).catch(() => {})
}

// 重新上架
const handleUp = (row: ProductVO) => {
    ElMessageBox.confirm(`确定重新上架 "${row.title}" 吗？上架后需重新审核。`, '上架确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' })
        .then(async () => {
            try { await upProductApi(row.id!); ElMessage.success('已提交上架，等待审核'); fetchData() }
            catch (e: any) { ElMessage.error(e.message || '操作失败') }
        }).catch(() => {})
}

// 删除
const handleDelete = (row: ProductVO) => {
    ElMessageBox.confirm(`确定删除 "${row.title}" 吗？删除后不可恢复。`, '删除确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
        .then(async () => {
            try { await deleteMyProductApi(row.id!); ElMessage.success('已删除'); fetchData() }
            catch (e: any) { ElMessage.error(e.message || '删除失败') }
        }).catch(() => {})
}

// 图片操作
const onImageUpload: UploadProps['onSuccess'] = (response) => {
    if (!formData.images) formData.images = []
    const newImg: ProductImageDTO = {
        imageUrl: response.data.filePath,
        isCover: formData.images.length === 0 ? 1 : 0,
        sort: formData.images.length + 1
    }
    formData.images.push(newImg)
}

const beforeImageUpload: UploadProps['beforeUpload'] = (rawFile) => {
    const allowed = ['image/jpeg', 'image/jpg', 'image/png']
    if (!allowed.includes(rawFile.type)) { ElMessage.error('仅支持 JPG/PNG 格式'); return false }
    if (rawFile.size > 2 * 1024 * 1024) { ElMessage.error('单张不超过 2MB'); return false }
    return true
}

const setCover = (idx: number) => {
    if (!formData.images) return
    formData.images.forEach((img, i) => img.isCover = i === idx ? 1 : 0)
}

const removeImage = (idx: number) => {
    if (!formData.images) return
    const removed = formData.images[idx]
    formData.images.splice(idx, 1)
    // 如果删的是封面，设第一张为封面
    if (removed.isCover === 1 && formData.images.length > 0) {
        formData.images[0].isCover = 1
    }
}

// 提交
const handleSubmit = async () => {
    if (!formRef.value) return
    await formRef.value.validate(async (valid) => {
        if (!valid) return
        submitLoading.value = true
        try {
            if (isEditing.value) {
                const params: ProductUpdateDTO = {
                    id: formData.id, categoryId: formData.categoryId,
                    title: formData.title, description: formData.description,
                    price: formData.price, originalPrice: formData.originalPrice,
                    condition: formData.condition, tradeType: formData.tradeType,
                    images: formData.images
                }
                await updateProductApi(params)
                ElMessage.success('修改成功')
            } else {
                await addProductApi(formData as ProductAddDTO)
                ElMessage.success('发布成功，请等待审核')
            }
            closeDialog()
            fetchData()
        } catch (e: any) {
            ElMessage.error(e.message || '操作失败')
        } finally { submitLoading.value = false }
    })
}

const resetForm = () => {
    formData.id = undefined
    formData.categoryId = undefined
    formData.title = ''
    formData.description = ''
    formData.price = undefined
    formData.originalPrice = undefined
    formData.condition = 1
    formData.tradeType = 1
    formData.images = []
    formRef.value?.clearValidate()
}

const closeDialog = () => { dialogVisible.value = false; resetForm() }

onMounted(() => { fetchData(); loadCategories() })
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;
// =====================================================
// 📦 我的商品 — 与首页风格统一
// =====================================================

.my-products {
    // ====== 发布商品按钮（通用） ======
    .page-action-btn {
        height: 36px;
        padding: 0 22px;
        border-radius: 20px;
        font-weight: 600;
        font-size: 13px;
    }

    // ====== 搜索区 — 白色卡片风格 ======
    .search-section {
        background: #fff;
        border-radius: 14px;
        padding: 16px 20px;
        margin-bottom: 16px;

        .el-form--inline .el-form-item {
            margin-bottom: 0;
            margin-right: 16px;
        }

        :deep(.el-form-item__label) {
            font-size: 13px;
            font-weight: 500;
            color: #636366;
        }

        :deep(.el-input__wrapper) {
            border-radius: 8px;
            box-shadow: 0 0 0 1px #e5e5ea inset;
            transition: box-shadow 0.2s;

            &:hover {
                box-shadow: 0 0 0 1px #d4d4d8 inset;
            }

            &.is-focus {
                box-shadow: 0 0 0 1px #1a1a1a inset;
            }
        }

        .el-button {
            border-radius: 20px;
            font-weight: 500;
            font-size: 13px;
            padding: 8px 18px;

            &--primary {
                background: #1a1a1a;
                border-color: #1a1a1a;

                &:hover {
                    background: #333;
                    border-color: #333;
                }
            }
        }
    }

    // ====== 表格卡片 ======
    .table-card {
        background: #fff;
        border-radius: 14px;
        overflow: hidden;
        box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);

        // 纯净表格样式
        :deep(.el-table) {
            border: none;
            font-size: 13px;

            &::before {
                display: none;
            }

            .el-table__header-wrapper {
                tr {
                    th.el-table__cell {
                        background: #f8f8fa;
                        color: #636366;
                        font-weight: 600;
                        font-size: 12px;
                        letter-spacing: 0.3px;
                        border-bottom: none;

                        & > .cell {
                            padding: 10px 12px;
                        }
                    }
                }
            }

            .el-table__body-wrapper {
                tr {
                    td.el-table__cell {
                        color: #1a1a1a;
                        border-bottom: 1px solid #f0f0f4;

                        & > .cell {
                            padding: 10px 12px;
                        }
                    }

                    &:last-child td.el-table__cell {
                        border-bottom: none;
                    }

                    &:hover td.el-table__cell {
                        background: #f8f8fa;
                    }
                }
            }

            .el-table__inner-wrapper::after {
                display: none;
            }

            .el-table__body-wrapper::-webkit-scrollbar {
                width: 6px;
            }
            .el-table__body-wrapper::-webkit-scrollbar-thumb {
                background: #d4d4d8;
                border-radius: 3px;
            }
        }

        // 操作按钮
        :deep(.el-button--primary.is-link) { font-size: 12px; font-weight: 500; }
        :deep(.el-button--warning.is-link) { font-size: 12px; font-weight: 500; }
        :deep(.el-button--success.is-link) { font-size: 12px; font-weight: 500; }
        :deep(.el-button--danger.is-link) { font-size: 12px; font-weight: 500; }

        // 标签
        :deep(.el-tag) {
            border-radius: 10px;
            font-size: 11px;
            font-weight: 500;
            padding: 0 10px;
            height: 22px;
            line-height: 22px;
        }
    }

    // ====== 分页 ======
    .pagination-wrapper {
        display: flex;
        justify-content: flex-end;
        padding: 16px 20px;
        background: #fff;
        border-top: 1px solid #f0f0f4;
    }

    // ====== 价格 ======
    .price-text {
        color: $color-accent;
        font-weight: 700;
        font-size: 15px;
    }

    .no-cover {
        color: #c0c4cc;
        font-size: 12px;
    }

    // ====== 弹窗底部 ======
    .dialog-footer {
        text-align: right;
    }

    // ====== 图片上传区 ======
    .image-upload-area {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;

        .uploaded-image {
            position: relative;
            width: 90px;
            height: 90px;
            border-radius: 6px;
            overflow: hidden;
            border: 1px solid #e8ecf1;

            .img-preview { width: 100%; height: 100%; }

            .img-actions {
                position: absolute;
                bottom: 0;
                left: 0;
                right: 0;
                background: rgba(0, 0, 0, 0.55);
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 6px;
                padding: 2px 4px;
            }
        }

        .upload-trigger {
            width: 90px;
            height: 90px;
            border: 1px dashed #c0c4cc;
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            color: #c0c4cc;
            transition: border-color 0.2s, color 0.2s;

            &:hover {
                border-color: #1a1a1a;
                color: #1a1a1a;
            }
        }
    }

    .upload-tip {
        margin-top: 6px;
        font-size: 12px;
        color: #909399;
    }
}
</style>
