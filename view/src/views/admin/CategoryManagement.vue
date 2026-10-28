<template>
    <div class="category-management">
        <!-- 搜索表单 -->
        <el-card class="search-card" shadow="never">
            <el-form :model="queryParams" inline>
                <el-form-item label="分类名称">
                    <el-input v-model="queryParams.name" placeholder="请输入分类名称" clearable style="width: 160px;" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px;">
                        <el-option label="启用" :value="1" />
                        <el-option label="禁用" :value="0" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
                    <el-button @click="handleReset" :icon="RefreshLeft">重置</el-button>
                    <el-button type="success" @click="handleAdd" :icon="Plus">新增顶级分类</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 操作提示 -->
        <el-alert
            title="拖拽行即可调整排序，拖动到其他分类下方可更改父分类"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 16px;"
        />

        <!-- 树形数据表格 -->
        <el-card class="table-card" shadow="never">
            <el-table
                ref="tableRef"
                :data="filteredTreeData"
                v-loading="loading"
                row-key="id"
                :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
                default-expand-all
                border
                stripe
                style="width: 100%"
                highlight-current-row
                @row-click="handleRowClick"
            >
                <el-table-column label="分类名称" min-width="250">
                    <template #default="{ row }">
                        <div class="tree-node-wrapper" :style="{ paddingLeft: row._level ? (row._level - 1) * 0 + 'px' : '0' }">
                            <!-- 拖拽手柄 -->
                            <span class="drag-handle" title="拖拽排序">
                                <el-icon><Rank /></el-icon>
                            </span>
                            <span class="node-name">
                                <el-icon v-if="row.status === 1" color="#67C23A"><CircleCheck /></el-icon>
                                <el-icon v-else color="#F56C6C"><CircleClose /></el-icon>
                                {{ row.name }}
                            </span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="sort" label="排序权重" width="100" align="center" />
                <el-table-column prop="status" label="状态" width="80" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain" size="small">
                            {{ row.status === 1 ? '启用' : '禁用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" min-width="170" align="center" />
                <el-table-column label="操作" width="220" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" link size="small" @click.stop="handleEdit(row)" :icon="EditPen">编辑</el-button>
                        <el-button type="success" link size="small" @click.stop="handleAddSub(row)" :icon="Plus">添加子级</el-button>
                        <el-button type="danger" link size="small" @click.stop="handleDelete(row)" :icon="Delete">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 新增/编辑弹窗 -->
        <el-dialog
            v-model="dialogVisible"
            :title="dialogTitle"
            width="550px"
            :before-close="handleDialogClose"
            destroy-on-close
        >
            <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
                <el-form-item label="父级分类">
                    <el-tree-select
                        v-model="formData.parentId"
                        :data="parentTreeOptions"
                        :props="{
                            value: 'id',
                            label: 'name',
                            children: 'children',
                            disabled: 'disabled'
                        }"
                        placeholder="不选则为顶级分类"
                        clearable
                        filterable
                        popper-class="category-tree-popper"
                        style="width: 100%;"
                        :render-after-expand="false"
                    />
                </el-form-item>
                <el-form-item label="分类名称" prop="name">
                    <el-input v-model="formData.name" placeholder="请输入分类名称" maxlength="50" />
                </el-form-item>
                <el-form-item label="排序权重" prop="sort">
                    <el-input-number v-model="formData.sort" :min="0" :max="99999" />
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="formData.status">
                        <el-radio :value="1">启用</el-radio>
                        <el-radio :value="0">禁用</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="handleDialogClose">取消</el-button>
                    <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, RefreshLeft, Plus, EditPen, Delete, Rank, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import Sortable from 'sortablejs'
import {
    categoryListAllApi,
    addCategoryApi,
    updateCategoryApi,
    deleteCategoryByIdApi,
    updateCategorySortApi
} from '@/api/category-api'
import type { CategoryVO, CategoryAddDTO, CategoryUpdateDTO, CategorySortDTO } from '@/types/category'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEditing = ref(false)
const isAddSub = ref(false)
const tableData = ref<CategoryVO[]>([])
const formRef = ref<FormInstance>()
const tableRef = ref()

let sortableInstance: Sortable | null = null

// 搜索条件
const queryParams = reactive({
    name: '',
    status: undefined as number | undefined
})

// 表单数据
const formData = reactive<CategoryAddDTO & { id?: number }>({
    id: undefined,
    parentId: undefined,
    name: '',
    sort: 0,
    status: 1
})

// 表单验证规则
const formRules: FormRules = {
    name: [
        { required: true, message: '请输入分类名称', trigger: 'blur' },
        { max: 50, message: '分类名称不能超过50个字符', trigger: 'blur' }
    ],
    sort: [
        { type: 'number', message: '排序必须为数字', trigger: 'blur' }
    ]
}

// 弹窗标题
const dialogTitle = computed(() => {
    if (isEditing.value) return '编辑分类'
    if (isAddSub.value) return '添加子分类'
    return '新增顶级分类'
})

// ========== 树形数据处理 ==========

/**
 * 将扁平列表转换为树形结构
 */
const buildTree = (flatList: CategoryVO[]): CategoryVO[] => {
    const map = new Map<number, CategoryVO>()
    const roots: CategoryVO[] = []

    // 先放入 map
    flatList.forEach(item => {
        map.set(item.id!, { ...item, children: [] })
    })

    // 构建层级关系
    flatList.forEach(item => {
        const node = map.get(item.id!)!
        if (item.parentId && item.parentId !== 0 && map.has(item.parentId)) {
            const parent = map.get(item.parentId)!
            parent.children = parent.children || []
            parent.children.push(node)
        } else {
            roots.push(node)
        }
    })

    // 计算层级深度
    const assignLevel = (nodes: CategoryVO[], level: number) => {
        nodes.forEach(node => {
            node._level = level
            if (node.children && node.children.length > 0) {
                assignLevel(node.children, level + 1)
            }
        })
    }
    assignLevel(roots, 1)

    return roots
}

/**
 * 扁平化树形结构（用于拖拽后生成新的sort值）
 */
const flattenTree = (nodes: CategoryVO[]): CategoryVO[] => {
    const result: CategoryVO[] = []
    const walk = (list: CategoryVO[]) => {
        list.forEach(node => {
            result.push(node)
            if (node.children && node.children.length > 0) {
                walk(node.children)
            }
        })
    }
    walk(nodes)
    return result
}

/**
 * 递归从树中移除指定ID的节点及其所有后代（用于编辑弹窗的父级选项过滤）
 */
const collectDescendantIds = (nodes: CategoryVO[], targetId: number): Set<number> => {
    const ids = new Set<number>()
    const walk = (list: CategoryVO[]) => {
        for (const node of list) {
            if (node.id === targetId) {
                ids.add(node.id!)
                if (node.children) {
                    const childIds = collectAllIds(node.children)
                    childIds.forEach(id => ids.add(id))
                }
                return
            }
            if (node.children) {
                walk(node.children)
            }
        }
    }
    walk(nodes)
    return ids
}

const collectAllIds = (nodes: CategoryVO[]): Set<number> => {
    const ids = new Set<number>()
    nodes.forEach(n => {
        ids.add(n.id!)
        if (n.children) collectAllIds(n.children).forEach(id => ids.add(id))
    })
    return ids
}

// 搜索过滤后的树形数据
const filteredTreeData = computed(() => {
    if (!queryParams.name && queryParams.status === undefined) {
        return tableData.value
    }
    // 深度过滤
    const filterNode = (nodes: CategoryVO[]): CategoryVO[] => {
        const result: CategoryVO[] = []
        for (const node of nodes) {
            let match = true
            if (queryParams.name && node.name) {
                match = match && node.name.includes(queryParams.name)
            }
            if (queryParams.status !== undefined) {
                match = match && node.status === queryParams.status
            }
            const filteredChildren = node.children ? filterNode(node.children) : []
            if (match || filteredChildren.length > 0) {
                result.push({
                    ...node,
                    children: filteredChildren.length > 0 ? filteredChildren : node.children
                })
            }
        }
        return result
    }
    return filterNode(tableData.value)
})

// 弹窗中的父级分类树选项
const parentTreeOptions = computed(() => {
    if (!isEditing.value) {
        return deepCloneTree(tableData.value)
    }
    // 编辑模式下，排除当前节点及其所有子节点
    const excludeIds = collectDescendantIds(tableData.value, formData.id!)
    // 创建深拷贝并禁用排除节点
    const disableNodes = (nodes: CategoryVO[]): CategoryVO[] => {
        return nodes.map(node => {
            const cloned: CategoryVO = { ...node, children: undefined as CategoryVO[] | undefined }
            if (excludeIds.has(node.id!)) {
                cloned.disabled = true
            }
            if (node.children && node.children.length > 0) {
                cloned.children = disableNodes(node.children)
            }
            return cloned
        })
    }
    return disableNodes(tableData.value)
})

const deepCloneTree = (nodes: CategoryVO[]): CategoryVO[] => {
    return nodes.map(n => ({
        ...n,
        children: n.children ? deepCloneTree(n.children) : undefined
    }))
}

// ========== 拖拽排序 ==========

const initSortable = () => {
    nextTick(() => {
        const el = document.querySelector('.el-table__body-wrapper tbody')
        if (!el) {
            // 如果还没渲染好，重试
            setTimeout(initSortable, 200)
            return
        }

        sortableInstance = Sortable.create(el as HTMLElement, {
            handle: '.drag-handle',
            animation: 200,
            ghostClass: 'sortable-ghost',
            dragClass: 'sortable-drag',
            onEnd: async (evt) => {
                if (evt.oldIndex === evt.newIndex) return

                loading.value = true
                try {
                    // 获取当前表格的显示顺序（flatTree 是按树展开顺序排列的）
                    const flatList = flattenTree(tableData.value)
                    // 重新分配 sort 值（每个间隔10，便于后续插入）
                    const sortList: CategorySortDTO[] = flatList.map((node, index) => ({
                        id: node.id!,
                        parentId: node.parentId ?? 0,
                        sort: (index + 1) * 10
                    }))

                    await updateCategorySortApi(sortList)
                    ElMessage.success('排序保存成功')
                    await loadData()
                } catch (error: any) {
                    ElMessage.error(error.message || '排序保存失败')
                } finally {
                    loading.value = false
                }
            }
        })
    })
}

const destroySortable = () => {
    if (sortableInstance) {
        sortableInstance.destroy()
        sortableInstance = null
    }
}

// ========== 数据加载 ==========

const loadData = async () => {
    try {
        loading.value = true
        const response = await categoryListAllApi()
        const flatList = response?.data ?? []
        tableData.value = buildTree(flatList)
    } catch (error: any) {
        ElMessage.error(error.message || '获取数据失败')
    } finally {
        loading.value = false
    }
}

// ========== 搜索 ==========

const handleSearch = () => {
    // 搜索基于已有数据进行过滤，computed 自动处理
}

const handleReset = () => {
    queryParams.name = ''
    queryParams.status = undefined
}

// ========== 行点击 ==========

const handleRowClick = (row: CategoryVO) => {
    // 预留：点击行可以展开/收起
}

// ========== 新增 ==========

const handleAdd = () => {
    isEditing.value = false
    isAddSub.value = false
    resetForm()
    formData.parentId = undefined
    dialogVisible.value = true
}

const handleAddSub = (row: CategoryVO) => {
    isEditing.value = false
    isAddSub.value = true
    resetForm()
    formData.parentId = row.id
    dialogVisible.value = true
}

// ========== 编辑 ==========

const handleEdit = (row: CategoryVO) => {
    isEditing.value = true
    isAddSub.value = false
    resetForm()
    formData.id = row.id
    formData.parentId = row.parentId ?? undefined
    formData.name = row.name ?? ''
    formData.sort = row.sort ?? 0
    formData.status = row.status ?? 1
    dialogVisible.value = true
}

// ========== 删除 ==========

const handleDelete = (row: CategoryVO) => {
    const rowId = row.id
    if (!rowId) return

    const hasChildren = row.children && row.children.length > 0
    let message = `确定要删除分类 "${row.name}" 吗？`
    if (hasChildren) {
        message = `"${row.name}" 下有子分类，删除后子分类将一并删除，确定继续吗？`
    }

    ElMessageBox.confirm(
        message,
        '删除确认',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(async () => {
        try {
            await deleteCategoryByIdApi(rowId)
            ElMessage.success('删除成功')
            await loadData()
        } catch (error: any) {
            ElMessage.error(error.message || '删除失败')
        }
    }).catch(() => {})
}

// ========== 表单 ==========

const resetForm = () => {
    formData.id = undefined
    formData.parentId = undefined
    formData.name = ''
    formData.sort = 0
    formData.status = 1
    formRef.value?.clearValidate()
}

const handleDialogClose = () => {
    dialogVisible.value = false
    resetForm()
}

const handleSubmit = async () => {
    if (!formRef.value) return

    await formRef.value.validate(async (valid) => {
        if (!valid) return

        try {
            submitLoading.value = true

            if (isEditing.value) {
                const params: CategoryUpdateDTO = {
                    id: formData.id,
                    parentId: formData.parentId ?? 0,
                    name: formData.name,
                    sort: formData.sort,
                    status: formData.status
                }
                await updateCategoryApi(params)
                ElMessage.success('修改成功')
            } else {
                const params: CategoryAddDTO = {
                    parentId: formData.parentId ?? 0,
                    name: formData.name,
                    sort: formData.sort,
                    status: formData.status
                }
                await addCategoryApi(params)
                ElMessage.success('新增成功')
            }

            handleDialogClose()
            await loadData()
        } catch (error: any) {
            ElMessage.error(error.message || (isEditing.value ? '修改失败' : '新增失败'))
        } finally {
            submitLoading.value = false
        }
    })
}

// ========== 生命周期 ==========

onMounted(async () => {
    await loadData()
    initSortable()
})

// 当 filteredTreeData 变化时（搜索/重置后），需要重新初始化 Sortable
import { watch } from 'vue'
watch(filteredTreeData, () => {
    nextTick(() => {
        destroySortable()
        initSortable()
    })
})

onBeforeUnmount(() => {
    destroySortable()
})
</script>

<style scoped lang="scss">
.category-management {
    .search-card {
        margin-bottom: 16px;

        :deep(.el-card__body) {
            padding-bottom: 0;
        }

        .el-form--inline .el-form-item {
            margin-bottom: 16px;
        }
    }

    .table-card {
        :deep(.el-card__body) {
            padding: 16px;
        }
    }

    // 表格 cell 弹性布局：解决树表格中缩进/图标/按钮不在同一行的问题
    :deep(.el-table__body-wrapper) {
        .el-table__row .cell,
        .el-table__row--level-0 .cell {
            display: flex !important;
            align-items: center !important;
            word-break: normal !important;
        }
    }

    .dialog-footer {
        text-align: right;
    }

    // 树节点样式
    .tree-node-wrapper {
        display: flex;
        align-items: center;
        gap: 6px;

        .drag-handle {
            cursor: grab;
            color: #999;
            display: inline-flex;
            align-items: center;
            padding: 2px;
            border-radius: 3px;
            transition: color 0.2s, background-color 0.2s;
            flex-shrink: 0;

            &:hover {
                color: #409eff;
                background-color: #ecf5ff;
            }

            &:active {
                cursor: grabbing;
            }
        }

        .node-name {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            font-size: 14px;
        }
    }
}

// 拖拽时的样式（全局样式，因组件内无法穿透）
:global(.sortable-ghost) {
    opacity: 0.4;
    background-color: #c8ebfb !important;
}

:global(.sortable-drag) {
    background-color: #fff !important;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
    opacity: 0.9;
}
</style>

<!-- 修复 el-tree-select 下拉框内 ▶ 图标与文字垂直不对齐 -->
<style lang="scss">
.category-tree-popper {
    .el-tree-node {
        // 重置节点默认 padding
        > .el-tree-node__content {
            height: auto !important;
            min-height: 34px;
            padding-right: 8px;

            // 展开/折叠图标
            .el-tree-node__expand-icon {
                display: flex !important;
                align-items: center !important;
                justify-content: center !important;
                width: 24px;
                height: 24px;
                margin: 0 !important;
                padding: 0 !important;
                font-size: 12px;
                line-height: 24px;
                flex-shrink: 0;
                color: #909399;
            }

            // 标签文字
            .el-tree-node__label {
                font-size: 14px;
                line-height: 34px;
                padding-left: 4px;
            }
        }

        // 叶子节点（无展开图标）补偿缩进
        &.is-leaf > .el-tree-node__content {
            padding-left: 24px !important;
        }
    }
}
</style>
