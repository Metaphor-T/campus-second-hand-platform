<template>
    <div class="user-layout">
        <!-- 顶部导航栏 -->
        <header class="navbar">
            <div class="navbar-inner">
                <div class="navbar-left">
                    <router-link to="/user-home/market" class="brand-link">
                        <img class="logo" src="/logo.png" alt="logo" />
                        <div class="brand-text">
                            <span class="brand">校园淘物</span>
                            <span class="brand-sub">Campus TaoWu</span>
                        </div>
                    </router-link>

                    <!-- 搜索栏 -->
                    <div class="navbar-search">
                        <div class="search-wrapper">
                            <el-icon class="search-icon"><Search /></el-icon>
                            <el-input
                                v-model="searchKeyword"
                                class="search-input"
                                placeholder="搜索商品、分类..."
                                clearable
                                @keyup.enter="doSearch"
                            />
                            <el-button class="search-btn" @click="doSearch">搜索</el-button>
                        </div>
                    </div>
                </div>

                <div class="navbar-right">
                    <router-link to="/user-home/chat" class="msg-link">
                        <span>消息中心</span>
                        <el-badge :value="totalUnread" :hidden="totalUnread === 0" :max="99" />
                    </router-link>
                    <el-dropdown trigger="click">
                        <span class="user-trigger">
                            <el-avatar class="nav-avatar" :size="36" v-if="baseUser.avatar" :src="getImageUrl(baseUser.avatar)" />
                            <el-avatar class="nav-avatar" :size="36" v-else>{{ (baseUser?.username ?? '用')[0] }}</el-avatar>
                            <span class="username">{{ baseUser?.username ?? '用户' }}</span>
                            <el-icon class="arrow"><ArrowDown /></el-icon>
                        </span>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item @click="openEditInfo" :icon="EditPen">修改信息</el-dropdown-item>
                                <el-dropdown-item @click="openChangePassword" :icon="TurnOff">修改密码</el-dropdown-item>
                                <el-dropdown-item divided @click="doLogout" :icon="SwitchButton">退出登录</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                </div>
            </div>

            <!-- 二级导航 -->
            <div class="nav-secondary">
                <div class="nav-secondary-inner">
                    <router-link to="/user-home/market" class="nav-link" active-class="nav-link-active">
                        <el-icon><ShoppingBag /></el-icon>首页
                    </router-link>
                    <router-link to="/user-home/products" class="nav-link" active-class="nav-link-active">
                        <el-icon><Goods /></el-icon>我的商品
                    </router-link>
                    <router-link to="/user-home/favorites" class="nav-link" active-class="nav-link-active">
                        <el-icon><Star /></el-icon>我的收藏
                    </router-link>
                    <router-link to="/user-home/orders" class="nav-link" active-class="nav-link-active">
                        <el-icon><List /></el-icon>订单中心
                    </router-link>
                    <router-link to="/user-home/my-reviews" class="nav-link" active-class="nav-link-active">
                        <el-icon><ChatLineSquare /></el-icon>我的评价
                    </router-link>
                    <router-link to="/user-home/address" class="nav-link" active-class="nav-link-active">
                        <el-icon><Location /></el-icon>收货地址
                    </router-link>
                </div>
            </div>
        </header>

        <!-- 主内容区域 -->
        <main class="main-content">
            <router-view />
        </main>

        <!-- ====== 修改信息弹窗 ====== -->
        <el-dialog
            v-model="dialogInfoVisible"
            title="修改个人信息"
            width="440px"
            :before-close="closeInfoDialog"
            destroy-on-close
            center
        >
            <div class="dialog-body">
                <div class="avatar-row">
                    <span class="label">头像</span>
                    <el-upload
                        class="avatar-uploader"
                        :action="uploadUrl"
                        :show-file-list="false"
                        :on-success="onAvatarSuccess"
                        :before-upload="beforeAvatarUpload"
                    >
                        <img v-if="imageUrl" :src="imageUrl" class="avatar-preview" />
                        <el-icon v-else class="avatar-placeholder"><Plus /></el-icon>
                    </el-upload>
                </div>
                <div class="field-row">
                    <span class="label">账号</span>
                    <el-input :model-value="baseUser.account" disabled />
                </div>
                <div class="field-row">
                    <span class="label">用户名</span>
                    <el-input v-model="infoForm.username" placeholder="请输入用户名" maxlength="20" />
                </div>
                <div class="field-row">
                    <span class="label">手机号</span>
                    <el-input v-model="infoForm.phone" placeholder="请输入手机号" maxlength="11" />
                </div>
                <div class="field-row">
                    <span class="label">性别</span>
                    <el-radio-group v-model="infoForm.gender">
                        <el-radio :value="1">男</el-radio>
                        <el-radio :value="2">女</el-radio>
                    </el-radio-group>
                </div>
            </div>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="closeInfoDialog">取消</el-button>
                    <el-button type="primary" :loading="infoLoading" @click="submitUpdateInfo">保存</el-button>
                </div>
            </template>
        </el-dialog>

        <!-- ====== 修改密码弹窗 ====== -->
        <el-dialog
            v-model="dialogPwdVisible"
            title="修改密码"
            width="400px"
            :before-close="closePwdDialog"
            destroy-on-close
            center
        >
            <div class="dialog-body">
                <div class="field-row">
                    <span class="label">新密码</span>
                    <el-input
                        v-model="pwdForm.password"
                        type="password"
                        placeholder="请输入新密码"
                        show-password
                    />
                </div>
            </div>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="closePwdDialog">取消</el-button>
                    <el-button type="primary" :loading="pwdLoading" @click="submitChangePwd">确认修改</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadProps } from 'element-plus'
import {
    ChatDotSquare, EditPen, TurnOff, SwitchButton, ArrowDown, Plus,
    Search, ShoppingBag, User, Goods, Star, List, ChatLineSquare, Location
} from '@element-plus/icons-vue'
import {
    clearRole,
    clearToken,
    clearLastVisitedPath,
    getToken
} from '@/utils/localStorage'
import {
    userTokenAuthApi,
    userUpdateInfoApi,
    updatePasswordApi,
    uploadAvatarApi
} from '@/api/user-api'
import { getTotalUnreadCountApi } from '@/api/chat-api'
import wsService from '@/utils/websocket'
import type { WebSocketMessage } from '@/types/chat'
import type { BaseUserVO, BaseUserUpdateDTO, UserUpdatePasswordDTO } from '@/types/user'

const router = useRouter()

// ====== 当前用户 ======
const baseUser = ref<BaseUserVO>({})

// ====== 搜索关键词 ======
const searchKeyword = ref('')

const doSearch = () => {
    const keyword = searchKeyword.value.trim()
    if (keyword) {
        router.push(`/user-home/market?search=${encodeURIComponent(keyword)}`)
    } else {
        router.push('/user-home/market')
    }
}

// ====== 未读消息数 ======
const totalUnread = ref(0)

// ====== WebSocket ======
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL as string

let removeWsListener: (() => void) | null = null

const initChatWs = () => {
    const token = getToken()
    if (!token || !apiBaseUrl) return
    wsService.connect(apiBaseUrl)
    removeWsListener = wsService.onMessage((msg: WebSocketMessage) => {
        if (msg.type === 'UNREAD_COUNT') {
            totalUnread.value = typeof msg.data === 'number' ? msg.data : 0
        }
    })
}

const fetchTotalUnread = async () => {
    try {
        const res = await getTotalUnreadCountApi()
        totalUnread.value = res?.data ?? 0
    } catch { /* 静默 */ }
}

// ====== 修改信息 ======
const dialogInfoVisible = ref(false)
const infoLoading = ref(false)
const imageUrl = ref('')
const infoForm = reactive<BaseUserUpdateDTO>({
    id: undefined,
    username: '',
    phone: '',
    gender: 1
})

const uploadUrl = `${import.meta.env.VITE_API_BASE_URL}/files/upload`

const getImageUrl = (filePath: string) => {
    if (!filePath) return ''
    return `${import.meta.env.VITE_API_BASE_URL}${filePath}`
}

const openEditInfo = () => {
    infoForm.id = baseUser.value.id
    infoForm.username = baseUser.value.username ?? ''
    infoForm.phone = baseUser.value.phone ?? ''
    infoForm.gender = baseUser.value.gender ?? 1
    imageUrl.value = getImageUrl(baseUser.value?.avatar ?? '')
    dialogInfoVisible.value = true
}

const closeInfoDialog = () => { dialogInfoVisible.value = false }

const onAvatarSuccess: UploadProps['onSuccess'] = async (response) => {
    try {
        imageUrl.value = getImageUrl(response.data.filePath)
        await uploadAvatarApi({ id: baseUser.value.id, avatar: response.data.filePath })
        ElMessage.success('头像已更新')
        await tokenAuth()
    } catch (e: any) {
        ElMessage.error(e.message || '头像上传失败')
    }
}

const beforeAvatarUpload: UploadProps['beforeUpload'] = (rawFile) => {
    const allowed = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp', 'image/bmp']
    if (!allowed.includes(rawFile.type)) {
        ElMessage.error('仅支持 JPG / PNG / GIF / WEBP / BMP 格式')
        return false
    }
    if (rawFile.size > 2 * 1024 * 1024) {
        ElMessage.error('图片大小不能超过 2MB')
        return false
    }
    return true
}

const submitUpdateInfo = async () => {
    try {
        infoLoading.value = true
        await userUpdateInfoApi({
            id: infoForm.id,
            username: infoForm.username,
            phone: infoForm.phone,
            gender: infoForm.gender
        })
        ElMessage.success('个人信息已保存')
        closeInfoDialog()
        await tokenAuth()
    } catch (e: any) {
        ElMessage.error(e.message || '保存失败')
    } finally {
        infoLoading.value = false
    }
}

// ====== 修改密码 ======
const dialogPwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdForm = reactive<UserUpdatePasswordDTO>({ password: '' })

const openChangePassword = () => {
    pwdForm.password = ''
    dialogPwdVisible.value = true
}

const closePwdDialog = () => {
    dialogPwdVisible.value = false
    pwdForm.password = ''
}

const submitChangePwd = async () => {
    if (!pwdForm.password) {
        ElMessage.warning('请输入新密码')
        return
    }
    try {
        pwdLoading.value = true
        if (!baseUser.value.id) return
        await updatePasswordApi(baseUser.value.id, pwdForm)
        ElMessage.success('密码修改成功，请重新登录')
        clearToken()
        clearRole()
        clearLastVisitedPath()
        router.push('/login')
    } catch (e: any) {
        ElMessage.error(e.message || '修改密码失败')
    } finally {
        pwdLoading.value = false
    }
}

// ====== 退出 ======
const doLogout = () => {
    ElMessageBox.confirm('确定退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        wsService.disconnect()
        clearToken()
        clearRole()
        clearLastVisitedPath()
        router.push('/guest-home')
        ElMessage.success('已退出')
    }).catch(() => {})
}

// ====== Token 认证 ======
const tokenAuth = async () => {
    try {
        const response = await userTokenAuthApi()
        baseUser.value = response?.data ?? {}
    } catch {
        router.push('/401')
        ElMessage.warning('身份认证异常，请重新登录')
    }
}

onMounted(() => {
    tokenAuth()
    fetchTotalUnread()
    initChatWs()
})

onUnmounted(() => {
    if (removeWsListener) {
        removeWsListener()
        removeWsListener = null
    }
})
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;
// ====== 布局 ======
.user-layout {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    background: #fff;
}

// ====== 顶部导航栏 ======
.navbar {
    position: sticky;
    top: 0;
    flex-shrink: 0;
    z-index: 100;
    background: #fff;
    margin: 12px 0;
}

.navbar-inner {
    display: flex;
    align-items: center;
    height: 68px;
    padding: 0 24px;
    max-width: $content-max-width;
    margin: 0 auto;
    gap: 24px;
}

// ----- 左侧品牌 + 搜索 -----
.navbar-left {
    display: flex;
    align-items: center;
    flex: 1;
    gap: 28px;

    .brand-link {
        display: flex;
        align-items: center;
        gap: 14px;
        text-decoration: none;
        flex-shrink: 0;
    }

    .logo {
        width: 52px;
        height: 52px;
        border-radius: 10px;
    }

    .brand-text {
        display: flex;
        flex-direction: column;
        line-height: 1.2;
    }

    .brand {
        font-size: 26px;
        font-weight: 700;
        color: #1a1a1a;
        letter-spacing: 0.5px;
    }

    .brand-sub {
        font-size: 12px;
        font-weight: 600;
        color: #636366;
        letter-spacing: 1.8px;
        text-transform: uppercase;
        font-style: italic;
        opacity: 0.75;
    }

    .navbar-search { margin-left: 0; }
}

// ----- 搜索栏 -----
.navbar-search {
    max-width: 440px;

    .search-wrapper {
        display: flex;
        align-items: center;
        background: #fff;
        border-radius: 24px;
        padding: 6px 6px 6px 16px;
        border: 1px solid #1a1a1a;
        transition: all 0.3s ease;

        &:focus-within {
            border-color: #1a1a1a;
            box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
        }

        .search-icon {
            color: #aeaeb2;
            font-size: 20px;
            flex-shrink: 0;
        }

        .search-input {
            flex: 1;
            :deep(.el-input__wrapper) {
                background: transparent !important;
                box-shadow: none !important;
                padding: 0 8px;
            }
            :deep(.el-input__inner) {
                font-size: 15px;
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
            height: 36px;
            padding: 0 20px;
            border-radius: 18px;
            font-size: 14px;
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

// ----- 右侧用户区 -----
.navbar-right {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 12px;

    .msg-link {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 16px;
        font-weight: 500;
        color: #1a1a1a;
        text-decoration: none;
        padding: 6px 12px;
        border-radius: 8px;
        background: #f0f0f0;
        transition: background 0.2s;

        &:hover {
            background: #e0e0e0;
        }

        :deep(.el-badge__content) {
            font-size: 11px;
            padding: 0 5px;
            height: 16px;
            line-height: 16px;
            min-width: 16px;
        }
    }

    .user-trigger {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;
        padding: 4px 10px;
        border-radius: 8px;
        transition: background 0.2s;

        &:hover {
            background: rgba(0, 0, 0, 0.04);
        }

        .nav-avatar {
            border: none;
        }

        .username {
            font-size: 18px;
            color: #1a1a1a;
            font-weight: 600;
            max-width: 180px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .arrow {
            color: #aeaeb2;
            font-size: 14px;
        }
    }
}

// ====== 二级导航 ======
.nav-secondary {
    background: #fff;
    overflow-x: auto;

    .nav-secondary-inner {
        display: flex;
        align-items: center;
        justify-content: flex-start;
        gap: 4px;
        max-width: $content-max-width;
        margin: 0 auto;
        padding: 14px 16px 14px;
        height: auto;

        .nav-link {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            font-size: 20px;
            font-weight: 500;
            color: #636366;
            text-decoration: none;
            padding: 10px 20px;
            border-radius: 24px;
            transition: all 0.25s ease;
            white-space: nowrap;

            .el-icon {
                font-size: 22px;
            }

            &:hover {
                color: #1a1a1a;
                background: rgba(0, 0, 0, 0.05);
            }
        }

        .nav-link-active {
            color: #fff;
            background: #1a1a1a;
            font-weight: 600;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);

            &:hover {
                background: #333;
                color: #fff;
                box-shadow: 0 3px 12px rgba(0, 0, 0, 0.25);
            }
        }
    }
}

// ====== 主内容区 ======
.main-content {
    flex: 1;
    overflow-y: auto;
    max-width: $content-max-width;
    width: 100%;
    margin: 0 auto;
    padding: 20px 16px;
}

// ====== 弹窗表单 ======
.dialog-body { padding: 0 8px; }

.avatar-row {
    display: flex;
    align-items: center;
    margin-bottom: 20px;

    .label {
        width: 64px;
        flex-shrink: 0;
        font-size: $font-size-base;
        color: $color-text-secondary;
    }
}

.avatar-uploader {
    :deep(.avatar-preview) {
        width: 60px;
        height: 60px;
        border-radius: $radius-round;
        object-fit: cover;
        border: 2px solid $color-border;
    }

    :deep(.avatar-placeholder) {
        width: 60px;
        height: 60px;
        border-radius: $radius-round;
        border: 2px dashed $color-text-light;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        color: $color-text-light;
        cursor: pointer;
        transition: border-color 0.2s, color 0.2s;
        &:hover {
            border-color: $color-primary;
            color: $color-primary;
        }
    }
}

.field-row {
    display: flex;
    align-items: center;
    margin-bottom: 18px;
    gap: 12px;

    .label {
        width: 64px;
        flex-shrink: 0;
        font-size: $font-size-base;
        color: $color-text-secondary;
        text-align: right;
    }

    .el-input, .el-radio-group { flex: 1; }
}

.dialog-footer { text-align: right; }

// ====== 响应式 ======
@media (max-width: 768px) {
    .navbar-inner { padding: 0 12px; gap: 10px; }
    .navbar-search { max-width: none; }
    .brand { display: none; }
    .nav-secondary-inner { padding: 0 8px; }
    .nav-secondary-inner .nav-link { padding: 6px 10px; font-size: 12px; }
    .main-content { padding: 12px 8px; }
}
</style>