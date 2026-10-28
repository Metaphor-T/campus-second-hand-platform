<template>
    <div class="admin-management">
        <!-- 搜索表单 -->
        <el-card class="search-card" shadow="never">
            <el-form :model="queryDTO" inline>
                <el-form-item label="账号">
                    <el-input v-model="queryDTO.account" placeholder="请输入账号" clearable style="width: 160px;" />
                </el-form-item>
                <el-form-item label="用户名">
                    <el-input v-model="queryDTO.username" placeholder="请输入用户名" clearable style="width: 160px;" />
                </el-form-item>
                <el-form-item label="手机号">
                    <el-input v-model="queryDTO.phone" placeholder="请输入手机号" clearable style="width: 160px;" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="queryDTO.loginStatus" placeholder="全部" clearable style="width: 120px;">
                        <el-option label="正常" :value="1" />
                        <el-option label="已封号" :value="2" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
                    <el-button @click="handleReset" :icon="RefreshLeft">重置</el-button>
                    <el-button type="success" @click="handleAdd" :icon="Plus">新增用户</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 数据表格 -->
        <el-card class="table-card" shadow="never">
            <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
                <el-table-column prop="account" label="账号" min-width="120" align="center" />
                <el-table-column prop="username" label="用户名" min-width="120" align="center" />
                <el-table-column label="头像" width="100" align="center">
                    <template #default="{ row }">
                        <el-avatar :size="40" :src="getImageUrl(row.avatar)" v-if="row.avatar" />
                        <el-avatar :size="40" v-else>{{ row.username?.charAt(0) || '用' }}</el-avatar>
                    </template>
                </el-table-column>
                <el-table-column prop="gender" label="性别" width="80" align="center">
                    <template #default="{ row }">
                        <span>{{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '-' }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="phone" label="手机号" min-width="130" align="center" />
                <el-table-column prop="loginStatus" label="状态" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.loginStatus === 1 ? 'success' : 'danger'" effect="plain">
                            {{ row.loginStatus === 1 ? '正常' : '已封号' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="220" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" link size="small" @click="handleEdit(row)" :icon="EditPen">编辑</el-button>
                        <el-button
                            :type="row.loginStatus === 1 ? 'warning' : 'success'"
                            link
                            size="small"
                            @click="handleToggleStatus(row)"
                        >
                            {{ row.loginStatus === 1 ? '封号' : '解封' }}
                        </el-button>
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

        <!-- 新增/编辑弹窗 -->
        <el-dialog
            v-model="dialogVisible"
            :title="isEditing ? '编辑用户' : '新增用户'"
            width="500px"
            :before-close="handleDialogClose"
            destroy-on-close
        >
            <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
                <el-form-item label="账号" prop="account" v-if="!isEditing">
                    <el-input v-model="formData.account" placeholder="请输入账号" />
                </el-form-item>
                <el-form-item label="密码" prop="password" v-if="!isEditing">
                    <el-input v-model="formData.password" type="password" placeholder="请输入密码" show-password />
                </el-form-item>
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="formData.username" placeholder="请输入用户名" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                    <el-input v-model="formData.phone" placeholder="请输入手机号" maxlength="11" />
                </el-form-item>
                <el-form-item label="性别" prop="gender">
                    <el-radio-group v-model="formData.gender">
                        <el-radio :value="1">男</el-radio>
                        <el-radio :value="2">女</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="状态" prop="loginStatus" v-if="isEditing">
                    <el-select v-model="formData.loginStatus" style="width: 120px;">
                        <el-option label="正常" :value="1" />
                        <el-option label="已封号" :value="2" />
                    </el-select>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, RefreshLeft, Plus, EditPen, Delete } from '@element-plus/icons-vue'
import {
    userListQueryApi,
    adduserApi,
    updateuserApi,
    deleteuserByIdApi,
    detailuserByIdApi
} from '@/api/user-api'
import type { BaseUserVO, BaseUser, BaseUserDTO } from '@/types/user'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEditing = ref(false)
const tableData = ref<BaseUserVO[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()

// 查询条件
const queryDTO = reactive<BaseUserDTO>({
    current: 1,
    size: 10,
    account: '',
    username: '',
    phone: '',
    loginStatus: undefined as number | undefined
})

// 表单数据
const formData = reactive<BaseUser>({
    id: undefined,
    account: '',
    password: '',
    username: '',
    phone: '',
    gender: 1,
    loginStatus: 1
})

// 表单验证规则
const formRules: FormRules = {
    account: [
        { required: true, message: '请输入账号', trigger: 'blur' },
        { min: 3, max: 20, message: '账号长度在3到20个字符', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
    ],
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
    ],
    phone: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ]
}

// 获取图片URL
const getImageUrl = (filePath: string) => {
    if (!filePath) return ''
    if (filePath.startsWith('http://') || filePath.startsWith('https://')) return filePath
    return `${import.meta.env.VITE_API_BASE_URL}${filePath}`
}

// 获取列表数据
const fetchData = async () => {
    try {
        loading.value = true
        const response = await userListQueryApi(queryDTO)
        tableData.value = response?.data ?? []
        total.value = response?.count ?? 0
    } catch (error: any) {
        ElMessage.error(error.message || '获取数据失败')
    } finally {
        loading.value = false
    }
}

// 搜索
const handleSearch = () => {
    queryDTO.current = 1
    fetchData()
}

// 重置
const handleReset = () => {
    queryDTO.account = ''
    queryDTO.username = ''
    queryDTO.phone = ''
    queryDTO.loginStatus = undefined
    queryDTO.current = 1
    fetchData()
}

// 新增
const handleAdd = () => {
    isEditing.value = false
    resetForm()
    dialogVisible.value = true
}

// 编辑
const handleEdit = async (row: BaseUserVO) => {
    isEditing.value = true
    resetForm()
    dialogVisible.value = true

    // 如果有ID，尝试获取详情
    if (row.id) {
        try {
            const response = await detailuserByIdApi(row.id)
            const detail = response?.data
            if (detail) {
                formData.id = detail.id
                formData.account = detail.account ?? ''
                formData.username = detail.username ?? ''
                formData.phone = detail.phone ?? ''
                formData.gender = detail.gender ?? 1
                formData.loginStatus = detail.loginStatus ?? 1
            }
        } catch {
            // 使用当前行数据
            Object.assign(formData, row)
        }
    }
}

// 删除
const handleDelete = (row: BaseUserVO) => {
    const rowId = row.id
    if (!rowId) return

    ElMessageBox.confirm(
        `确定要删除用户 "${row.username || row.account}" 吗？`,
        '删除确认',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(async () => {
        try {
            await deleteuserByIdApi(rowId)
            ElMessage.success('删除成功')
            fetchData()
        } catch (error: any) {
            ElMessage.error(error.message || '删除失败')
        }
    }).catch(() => {})
}

// 切换状态
const handleToggleStatus = (row: BaseUserVO) => {
    const rowId = row.id
    const rowUsername = row.username
    const rowPhone = row.phone
    const rowGender = row.gender
    const rowLoginStatus = row.loginStatus
    if (!rowId) return

    const action = rowLoginStatus === 1 ? '封号' : '解封'
    ElMessageBox.confirm(
        `确定要${action}用户 "${rowUsername || row.account}" 吗？`,
        `${action}确认`,
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(async () => {
        try {
            const updateData: BaseUser = {
                id: rowId,
                username: rowUsername,
                phone: rowPhone,
                gender: rowGender,
                loginStatus: rowLoginStatus === 1 ? 2 : 1
            }
            await updateuserApi(updateData)
            ElMessage.success(`${action}成功`)
            fetchData()
        } catch (error: any) {
            ElMessage.error(error.message || `${action}失败`)
        }
    }).catch(() => {})
}

// 重置表单
const resetForm = () => {
    formData.id = undefined
    formData.account = ''
    formData.password = ''
    formData.username = ''
    formData.phone = ''
    formData.gender = 1
    formData.loginStatus = 1
    formRef.value?.clearValidate()
}

// 关闭对话框
const handleDialogClose = () => {
    dialogVisible.value = false
    resetForm()
}

// 提交表单
const handleSubmit = async () => {
    if (!formRef.value) return

    await formRef.value.validate(async (valid) => {
        if (!valid) return

        try {
            submitLoading.value = true

            if (isEditing.value) {
                // 编辑模式
                await updateuserApi({
                    id: formData.id,
                    username: formData.username,
                    phone: formData.phone,
                    gender: formData.gender,
                    loginStatus: formData.loginStatus
                })
                ElMessage.success('修改成功')
            } else {
                // 新增模式
                await adduserApi({
                    account: formData.account,
                    password: formData.password,
                    username: formData.username,
                    phone: formData.phone,
                    gender: formData.gender
                })
                ElMessage.success('新增成功')
            }

            handleDialogClose()
            fetchData()
        } catch (error: any) {
            ElMessage.error(error.message || (isEditing.value ? '修改失败' : '新增失败'))
        } finally {
            submitLoading.value = false
        }
    })
}

onMounted(() => {
    fetchData()
})
</script>

<style scoped lang="scss">
.admin-management {
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

    .pagination-wrapper {
        display: flex;
        justify-content: flex-end;
        margin-top: 16px;
    }

    .dialog-footer {
        text-align: right;
    }
}
</style>