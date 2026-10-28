<template>
    <div class="admin-management">
        <!-- 搜索表单 -->
        <el-card class="search-card" shadow="never">
            <el-form :model="queryDTO" inline>
                <el-form-item label="发送者">
                    <el-input v-model="queryDTO.fromUsername" placeholder="发送者用户名" clearable style="width: 140px;" />
                </el-form-item>
                <el-form-item label="接收者">
                    <el-input v-model="queryDTO.toUsername" placeholder="接收者用户名" clearable style="width: 140px;" />
                </el-form-item>
                <el-form-item label="消息内容">
                    <el-input v-model="queryDTO.content" placeholder="消息内容" clearable style="width: 160px;" />
                </el-form-item>
                <el-form-item label="消息类型">
                    <el-select v-model="queryDTO.messageType" placeholder="全部" clearable style="width: 110px;">
                        <el-option label="文本" :value="1" />
                        <el-option label="表情" :value="2" />
                        <el-option label="图片" :value="3" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="queryDTO.status" placeholder="全部" clearable style="width: 110px;">
                        <el-option label="未读" :value="0" />
                        <el-option label="已读" :value="1" />
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
                <el-table-column label="ID" prop="id" width="70" align="center" />
                <el-table-column label="发送者" width="120" align="center">
                    <template #default="{ row }">
                        <div class="user-cell">
                            <el-avatar :size="26" :src="getImageUrl(row.fromAvatar)" v-if="row.fromAvatar" />
                            <span>{{ row.fromUsername || '-' }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="接收者" width="120" align="center">
                    <template #default="{ row }">
                        <div class="user-cell">
                            <el-avatar :size="26" :src="getImageUrl(row.toAvatar)" v-if="row.toAvatar" />
                            <span>{{ row.toUsername || '-' }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="消息内容" min-width="220">
                    <template #default="{ row }">
                        <span v-if="row.messageType === 3" class="image-msg">[图片]</span>
                        <span v-else-if="row.messageType === 2" class="emoji-msg">{{ row.content }}</span>
                        <span v-else class="text-msg">{{ row.content }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="消息类型" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag v-if="row.messageType === 1" size="small" effect="plain">文本</el-tag>
                        <el-tag v-else-if="row.messageType === 2" size="small" type="warning" effect="plain">表情</el-tag>
                        <el-tag v-else-if="row.messageType === 3" size="small" type="success" effect="plain">图片</el-tag>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="80" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain" size="small">
                            {{ row.status === 1 ? '已读' : '未读' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="发送时间" width="170" align="center">
                    <template #default="{ row }">
                        {{ row.createTime || '-' }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
                        <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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
        <el-dialog
            v-model="detailVisible"
            title="消息详情"
            width="520px"
            destroy-on-close
        >
            <el-descriptions :column="2" border v-if="detailData">
                <el-descriptions-item label="消息ID" :span="2">{{ detailData.id }}</el-descriptions-item>
                <el-descriptions-item label="发送者ID">{{ detailData.fromUserId }}</el-descriptions-item>
                <el-descriptions-item label="发送者">{{ detailData.fromUsername }}</el-descriptions-item>
                <el-descriptions-item label="接收者ID">{{ detailData.toUserId }}</el-descriptions-item>
                <el-descriptions-item label="接收者">{{ detailData.toUsername }}</el-descriptions-item>
                <el-descriptions-item label="消息类型" :span="2">
                    <el-tag v-if="detailData.messageType === 1" size="small" effect="plain">文本</el-tag>
                    <el-tag v-else-if="detailData.messageType === 2" size="small" type="warning" effect="plain">表情</el-tag>
                    <el-tag v-else-if="detailData.messageType === 3" size="small" type="success" effect="plain">图片</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="消息状态" :span="2">
                    <el-tag :type="detailData.status === 1 ? 'success' : 'info'" effect="plain">
                        {{ detailData.status === 1 ? '已读' : '未读' }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="发送时间" :span="2">{{ detailData.createTime }}</el-descriptions-item>
                <el-descriptions-item label="消息内容" :span="2">
                    <template v-if="detailData.messageType === 3">
                        <img :src="getImageUrl(detailData.content || '')" class="detail-image" />
                    </template>
                    <template v-else>
                        <span style="word-break: break-all;">{{ detailData.content }}</span>
                    </template>
                </el-descriptions-item>
            </el-descriptions>
            <template #footer>
                <el-button @click="detailVisible = false">关闭</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { messageListPageApi, deleteMessageApi } from '@/api/chat-api'
import type { ChatMessageVO } from '@/types/chat'

const loading = ref(false)
const tableData = ref<ChatMessageVO[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detailData = ref<ChatMessageVO | null>(null)

// 查询条件
const queryDTO = reactive({
    current: 1,
    size: 10,
    fromUsername: '',
    toUsername: '',
    content: '',
    messageType: undefined as number | undefined,
    status: undefined as number | undefined,
    startTime: undefined as string | undefined,
    endTime: undefined as string | undefined
})

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
        const response = await messageListPageApi(queryDTO)
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
    queryDTO.fromUsername = ''
    queryDTO.toUsername = ''
    queryDTO.content = ''
    queryDTO.messageType = undefined
    queryDTO.status = undefined
    queryDTO.current = 1
    fetchData()
}

// 查看详情
const handleDetail = (row: ChatMessageVO) => {
    detailData.value = { ...row }
    detailVisible.value = true
}

// 删除
const handleDelete = (row: ChatMessageVO) => {
    const rowId = row.id
    if (!rowId) return

    ElMessageBox.confirm(
        `确定要删除消息 (ID: ${rowId}) 吗？删除后不可恢复。`,
        '删除确认',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        }
    ).then(async () => {
        try {
            await deleteMessageApi(rowId)
            ElMessage.success('删除成功')
            fetchData()
        } catch (error: any) {
            ElMessage.error(error.message || '删除失败')
        }
    }).catch(() => {})
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

    .user-cell {
        display: flex;
        align-items: center;
        gap: 6px;
        justify-content: center;

        span {
            font-size: 13px;
        }
    }

    .text-msg {
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        font-size: 13px;
        color: #333;
    }

    .emoji-msg {
        font-size: 22px;
    }

    .image-msg {
        color: #409eff;
        font-size: 13px;
    }

    .detail-image {
        max-width: 100%;
        max-height: 300px;
        border-radius: 6px;
        display: block;
    }
}
</style>
