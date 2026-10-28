<template>
    <div class="user-address">
        <div class="page-header">
            <h3>收货地址</h3>
            <el-button class="add-btn" circle @click="openAddDialog" :icon="Plus" />
        </div>

        <!-- 地址列表 -->
        <el-empty v-if="addressList.length === 0" description="暂无收货地址，点击上方按钮添加" />

        <div v-else class="address-list">
            <div v-for="item in addressList" :key="item.id" class="address-card">
                <div class="address-info">
                    <div class="address-top">
                        <span class="consignee">{{ item.receiverName }}</span>
                        <span class="phone">{{ item.receiverPhone }}</span>
                        <el-tag v-if="item.isDefault === 1" size="small" type="primary" effect="plain">默认</el-tag>
                    </div>
                    <div class="address-detail">
                        {{ item.province }} {{ item.city }} {{ item.district }} {{ item.detailAddress }}
                    </div>
                    <div class="address-zip" v-if="item.postalCode">
                        邮编：{{ item.postalCode }}
                    </div>
                </div>
                <div class="address-actions">
                    <el-button v-if="item.isDefault !== 1" text type="primary" size="small" @click="handleSetDefault(item.id!)">
                        设为默认
                    </el-button>
                    <el-button text type="primary" size="small" @click="openEditDialog(item)">修改</el-button>
                    <el-button text type="danger" size="small" @click="handleDelete(item.id!)">删除</el-button>
                </div>
            </div>
        </div>

        <!-- 新增 / 修改弹窗 -->
        <el-dialog
            v-model="dialogVisible"
            :title="isEdit ? '修改地址' : '新增地址'"
            width="520px"
            :before-close="closeDialog"
            destroy-on-close
        >
            <el-form
                ref="formRef"
                :model="form"
                :rules="rules"
                label-width="90px"
                label-position="right"
            >
                <el-form-item label="收货人" prop="receiverName">
                    <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" maxlength="20" />
                </el-form-item>

                <el-form-item label="手机号" prop="receiverPhone">
                    <el-input v-model="form.receiverPhone" placeholder="请输入手机号" maxlength="11" />
                </el-form-item>

                <el-row :gutter="10">
                    <el-col :span="8">
                        <el-form-item label="省份" prop="province" label-width="90px">
                            <el-input v-model="form.province" placeholder="如：广东省" maxlength="20" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="城市" prop="city" label-width="60px">
                            <el-input v-model="form.city" placeholder="如：广州市" maxlength="20" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="区县" prop="district" label-width="60px">
                            <el-input v-model="form.district" placeholder="如：天河区" maxlength="20" />
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-form-item label="详细地址" prop="detailAddress">
                    <el-input
                        v-model="form.detailAddress"
                        placeholder="请输入街道、门牌号等详细信息"
                        type="textarea"
                        :rows="2"
                        maxlength="100"
                        show-word-limit
                    />
                </el-form-item>

                <el-form-item label="邮政编码" prop="postalCode">
                    <el-input v-model="form.postalCode" placeholder="选填" maxlength="6" />
                </el-form-item>

                <el-form-item label="设为默认">
                    <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
                </el-form-item>
            </el-form>

            <template #footer>
                <el-button @click="closeDialog">取消</el-button>
                <el-button type="primary" :loading="loading" @click="submit">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
    listAddressApi,
    addAddressApi,
    updateAddressApi,
    deleteAddressApi,
    setDefaultAddressApi
} from '@/api/address-api'
import type { AddressVO, AddressAddDTO } from '@/types/address'

const addressList = ref<AddressVO[]>([])

// ====== 弹窗 ======
const dialogVisible = ref(false)
const isEdit = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<AddressAddDTO>({
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    postalCode: '',
    isDefault: 0
})

const editId = ref<number | undefined>(undefined)

const rules: FormRules = {
    receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
    receiverPhone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
    province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
    city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
    district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
    detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

// ====== 列表加载 ======
const loadList = async () => {
    try {
        const response = await listAddressApi()
        addressList.value = response?.data ?? []
    } catch {
        // 父级已处理异常
    }
}

// ====== 新增 ======
const openAddDialog = () => {
    isEdit.value = false
    editId.value = undefined
    form.receiverName = ''
    form.receiverPhone = ''
    form.province = ''
    form.city = ''
    form.district = ''
    form.detailAddress = ''
    form.postalCode = ''
    form.isDefault = 0
    dialogVisible.value = true
}

// ====== 修改 ======
const openEditDialog = (item: AddressVO) => {
    isEdit.value = true
    editId.value = item.id
    form.receiverName = item.receiverName ?? ''
    form.receiverPhone = item.receiverPhone ?? ''
    form.province = item.province ?? ''
    form.city = item.city ?? ''
    form.district = item.district ?? ''
    form.detailAddress = item.detailAddress ?? ''
    form.postalCode = item.postalCode ?? ''
    form.isDefault = item.isDefault ?? 0
    dialogVisible.value = true
}

// ====== 提交 ======
const submit = async () => {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return

    try {
        loading.value = true
        if (isEdit.value) {
            await updateAddressApi({
                id: editId.value,
                receiverName: form.receiverName,
                receiverPhone: form.receiverPhone,
                province: form.province,
                city: form.city,
                district: form.district,
                detailAddress: form.detailAddress,
                postalCode: form.postalCode,
                isDefault: form.isDefault
            })
            ElMessage.success('地址已更新')
        } else {
            await addAddressApi({
                receiverName: form.receiverName,
                receiverPhone: form.receiverPhone,
                province: form.province,
                city: form.city,
                district: form.district,
                detailAddress: form.detailAddress,
                postalCode: form.postalCode,
                isDefault: form.isDefault
            })
            ElMessage.success('地址已添加')
        }
        dialogVisible.value = false
        await loadList()
    } catch (e: any) {
        ElMessage.error(e.message || '操作失败')
    } finally {
        loading.value = false
    }
}

// ====== 关闭弹窗 ======
const closeDialog = () => {
    dialogVisible.value = false
}

// ====== 设为默认 ======
const handleSetDefault = async (id: number) => {
    try {
        await setDefaultAddressApi(id)
        ElMessage.success('已设为默认地址')
        await loadList()
    } catch (e: any) {
        ElMessage.error(e.message || '操作失败')
    }
}

// ====== 删除 ======
const handleDelete = async (id: number) => {
    try {
        await ElMessageBox.confirm('确定删除该收货地址吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })
        await deleteAddressApi(id)
        ElMessage.success('地址已删除')
        await loadList()
    } catch {
        // 取消删除不做任何操作
    }
}

onMounted(() => {
    loadList()
})
</script>

<style scoped lang="scss">
.user-address {
    margin: 0 auto;
    padding-bottom: 80px;
}

.page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
    padding: 0 20px;

    h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        color: #303133;
    }

    .add-btn {
        width: 44px;
        height: 44px;
        background: #1a1a1a;
        color: #fff;
        border: none;
        box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
        font-size: 20px;
        transition: all 0.25s ease;

        &:hover {
            background: #333;
            box-shadow: 0 6px 20px rgba(0, 0, 0, 0.35);
            transform: scale(1.05);
        }
    }
}

.address-list {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    gap: 12px;
    padding: 0 20px;
}

.address-card {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    background: #fff;
    border: 1px solid #e8ecf1;
    border-radius: 8px;
    padding: 16px 20px;
    transition: box-shadow 0.2s;

    &:hover {
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    }
}

.address-info {
    flex: 1;
    min-width: 0;
}

.address-top {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;

    .consignee {
        font-size: 15px;
        font-weight: 500;
        color: #303133;
    }

    .phone {
        font-size: 14px;
        color: #909399;
    }
}

.address-detail {
    font-size: 14px;
    color: #606266;
    line-height: 1.5;
}

.address-zip {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
}

.address-actions {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
    margin-top: 12px;
    padding-top: 10px;
    border-top: 1px solid #f0f0f0;
}
</style>
