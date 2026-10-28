<template>
    <div class="admin-layout">
        <!-- 左侧菜单 -->
        <div class="sidebar" :style="{ width: sidebarWidth + 'px' }" :class="{ 'collapsed': isCollapsed }">
            <div class="sidebar-header">
                <img style="width: 30px; height: 30px;" src="/logo.png" alt="">
                <h2 v-if="!isCollapsed">校园交易平台</h2>
            </div>
            <div class="sidebar-menu">
                <ul class="nav-list">
                    <template v-for="item in sidebarItems" :key="item.path">
                        <!-- 有子菜单的分组 -->
                        <li v-if="item.children" class="nav-group">
                            <div class="nav-group-header" @click="toggleExpand(item.path)">
                                <el-icon v-if="item.icon && getIconComponent(item.icon)">
                                    <component :is="getIconComponent(item.icon)" />
                                </el-icon>
                                <span v-if="!isCollapsed">{{ item.name }}</span>
                                <el-icon v-if="!isCollapsed" class="expand-icon" :class="{ expanded: expandedSet.has(item.path) }">
                                    <ArrowDown />
                                </el-icon>
                            </div>
                            <ul v-show="isCollapsed || expandedSet.has(item.path)" class="nav-children">
                                <li v-for="child in item.children" :key="child.path"
                                    :class="{ active: currentRoute === child.path }"
                                    @click="$router.push(child.path)"
                                    :title="isCollapsed ? child.name : undefined">
                                    <span>{{ child.name }}</span>
                                </li>
                            </ul>
                        </li>
                        <!-- 普通独立菜单项 -->
                        <li v-else
                            :class="{ active: currentRoute === item.path }"
                            @click="$router.push(item.path)"
                            :title="isCollapsed ? (item.name as string) : undefined">
                            <el-icon v-if="item.icon && getIconComponent(item.icon)">
                                <component :is="getIconComponent(item.icon)" />
                            </el-icon>
                            <span v-if="!isCollapsed">{{ item.name }}</span>
                        </li>
                    </template>
                </ul>
            </div>
        </div>

        <!-- 拖拽条 -->
        <div class="drag-bar" @mousedown="startDrag" @dblclick="toggleCollapse"></div>

        <!-- 右侧内容区域 -->
        <div class="main-content">
            <!-- 顶部导航 -->
            <div class="header">
                <div class="header-left">
                    <el-breadcrumb separator="">
                        <el-breadcrumb-item v-for="(item, index) in breadcrumbList" :key="item.path">
                            <span class="path-name" v-if="index === breadcrumbList.length - 1">
                                {{ item.meta?.title || item.name }}
                            </span>
                            <router-link v-else :to="item.path">
                                {{ item.meta?.title || item.name }}
                            </router-link>
                        </el-breadcrumb-item>
                    </el-breadcrumb>
                </div>
                <div class="header-right">
                    <el-dropdown class="custom-dropdown">
                        <span class="user-info">
                            <el-avatar :size="32" v-if="baseUser.avatar" :src="getImageUrl(baseUser.avatar)" />
                            <el-avatar size="32" v-else>{{ baseUser?.username ?? '牛' }}</el-avatar>
                            <span class="username">{{ baseUser?.username ?? '待设置昵称' }}</span>
                            <el-icon><ArrowDownBold /></el-icon>
                        </span>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item @click="updateInfo" :icon="EditPen">修改信息</el-dropdown-item>
                                <el-dropdown-item @click="updatePassword" :icon="TurnOff">修改密码</el-dropdown-item>
                                <el-dropdown-item @click="loginout" :icon="Operation">退出登录</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                </div>
            </div>

            <!-- 页面内容 -->
            <div class="content">
                <router-view />
            </div>
        </div>

        <!-- 修改信息弹窗（原始逻辑） -->
        <el-dialog v-model="dialogUpdateInfoVisible" title="修改个人信息" width="400"
            :before-close="handleUpdateInfoDialogClose">
            <div>
                <div>
                    <p>头像</p>
                    <el-upload class="avatar-uploader" action="http://localhost:21090/files/upload"
                        :show-file-list="false" :on-success="handleAvatarSuccess"
                        :before-upload="beforeAvatarUpload">
                        <img v-if="imageUrl" :src="imageUrl" class="avatar" />
                        <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
                    </el-upload>
                </div>
                <div style="margin-block: 8px;">
                    <p>用户名</p>
                    <el-input v-model="updateInfoDTO.username" placeholder="输入用户名" />
                </div>
                <div style="margin-block: 8px;">
                    <p>手机号</p>
                    <el-input v-model="updateInfoDTO.phone" placeholder="输入手机号" />
                </div>
                <div style="margin-block: 8px;">
                    <p>性别</p>
                    <el-radio-group v-model="updateInfoDTO.gender">
                        <el-radio :value="1">男</el-radio>
                        <el-radio :value="2">女</el-radio>
                    </el-radio-group>
                </div>
            </div>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="handleUpdateInfoDialogClose">取消操作</el-button>
                    <el-button type="primary" :loading-icon="Eleme" :loading="loadingUpdatePassword"
                        @click="handleUpdateInfoOperation">
                        我确定修改
                    </el-button>
                </div>
            </template>
        </el-dialog>

        <!-- 修改密码弹窗（原始逻辑） -->
        <el-dialog v-model="dialogUpdatePasswordVisible" title="修改密码" width="400"
            :before-close="handlePasswordDialogClose">
            <div>
                <el-input v-model="updatePasswordDTO.password" type="password" placeholder="请输入新密码" show-password />
            </div>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="handlePasswordDialogClose">取消操作</el-button>
                    <el-button type="primary" :loading-icon="Eleme" :loading="loadingUpdatePassword"
                        @click="handleUpdatePassword">
                        我确定修改
                    </el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadProps } from 'element-plus'
import type { Action } from 'element-plus'
import { EditPen, TurnOff, Operation, ArrowDownBold, ArrowDown, Eleme, Plus, Compass as CompassIcon, User as UserIcon, UserFilled as UserFilledIcon, Grid as GridIcon, Goods as GoodsIcon, Tickets as TicketsIcon, ChatDotSquare as ChatDotSquareIcon } from "@element-plus/icons-vue"
import { ref, reactive, computed, onMounted, onUnmounted, watch } from "vue"
import { useRouter, useRoute } from 'vue-router'
import {
    getToken,
    clearRole,
    clearToken,
    clearLastVisitedPath,
    getSidebarWidth,
    setSidebarWidth,
    getSidebarCollapsed,
    setSidebarCollapsed
} from '@/utils/localStorage'
import { adminTokenAuthApi, updatePasswordApi, uploadAvatarApi, adminUpdateInfoApi } from "@/api/admin-api"
import { BaseUserVO, UserUpdatePasswordDTO, BaseUserUpdateDTO } from '@/types/user'

const router = useRouter()
const route = useRoute()

// 图标名称 → 组件映射
const iconMap: Record<string, any> = {
    'Compass': CompassIcon,
    'Avatar': UserIcon,
    'UserFilled': UserFilledIcon,
    'Grid': GridIcon,
    'Goods': GoodsIcon,
    'Tickets': TicketsIcon,
    'ChatDotSquare': ChatDotSquareIcon,
}

// 获取图标组件
const getIconComponent = (iconName: string) => iconMap[iconName] || null

// ========== 原始页面数据与逻辑 ==========
const baseUser = ref<BaseUserVO>({})
const updatePasswordDTO = ref<UserUpdatePasswordDTO>({})
const updateInfoDTO = ref<BaseUserUpdateDTO>({})
const dialogUpdatePasswordVisible = ref(false)
const dialogUpdateInfoVisible = ref(false)
const loadingUpdatePassword = ref(false)
const imageUrl = ref('')

const handleUpdateInfoDialogClose = () => {
    dialogUpdateInfoVisible.value = false
    updatePasswordDTO.value.password = ''
    imageUrl.value = ''
}

const getImageUrl = (filePath: string) => {
    if (!filePath) return ''
    if (filePath.startsWith('http://') || filePath.startsWith('https://')) return filePath
    return `${import.meta.env.VITE_API_BASE_URL}${filePath}`
}

const handleAvatarSuccess: UploadProps['onSuccess'] = async (response) => {
    try {
        imageUrl.value = getImageUrl(response.data.filePath)
        const baseUserUpdateDTO = {
            id: baseUser.value.id,
            avatar: response.data.filePath
        }
        await uploadAvatarApi(baseUserUpdateDTO)
        ElMessage.success(`头像设置成功`)
    } catch (error: any) {
        ElMessage.error(`${error.message}`)
    }
}

const beforeAvatarUpload: UploadProps['beforeUpload'] = (rawFile) => {
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp', 'image/bmp']
    const maxSize = 2 * 1024 * 1024 // 2MB

    if (!allowedTypes.includes(rawFile.type)) {
        ElMessage.error('Avatar picture must be JPG, PNG, GIF, WEBP or BMP format!')
        return false
    } else if (rawFile.size > maxSize) {
        ElMessage.error('Avatar picture size can not exceed 2MB!')
        return false
    }
    return true
}

const handlePasswordDialogClose = () => {
    dialogUpdatePasswordVisible.value = false
    updatePasswordDTO.value = {}
}

// 修改信息
const updateInfo = () => {
    updateInfoDTO.value = { ...baseUser.value }
    imageUrl.value = getImageUrl(baseUser.value?.avatar ?? '')
    dialogUpdateInfoVisible.value = true
}

// 修改密码
const updatePassword = () => {
    dialogUpdatePasswordVisible.value = true
}

// 退出登录
const loginout = () => {
    ElMessageBox.alert('您确定退出登录?', '操作确认', {
        confirmButtonText: 'OK',
        callback: (action: Action) => {
            if (action === 'confirm') {
                clearRole()
                clearToken()
                clearLastVisitedPath()
                router.push('/guest-home')
                ElMessage.success('退出登录成功')
            }
        },
    })
}

const handleUpdateInfoOperation = async () => {
    try {
        const baseUserUpdateDTO = {
            id: baseUser.value.id,
            username: updateInfoDTO.value.username,
            phone: updateInfoDTO.value.phone,
            gender: updateInfoDTO.value.gender
        }
        await adminUpdateInfoApi(baseUserUpdateDTO)
        ElMessage.success(`个人信息修改成功`)
        handleUpdateInfoDialogClose()
        await tokenAuth()
    } catch (error: any) {
        ElMessage.error(`${error.message}`)
    } finally {
        loadingUpdatePassword.value = false
    }
}

const handleUpdatePassword = async () => {
    try {
        loadingUpdatePassword.value = true
        if (!baseUser.value.id) return
        await updatePasswordApi(baseUser.value.id, updatePasswordDTO.value)
        loadingUpdatePassword.value = false
        router.push('/login')
        clearRole()
        clearToken()
        ElMessage.success(`修改密码成功,请重新登录`)
        return
    } catch (error: any) {
        ElMessage.error(`${error.message}`)
    } finally {
        loadingUpdatePassword.value = false
    }
}

// token校验
const tokenAuth = async () => {
    try {
        const response = await adminTokenAuthApi()
        baseUser.value = response?.data ?? {}
    } catch (error: any) {
        router.push('/401')
        ElMessage.warning(`身份认证异常`)
        return
    }
}
// ========== 原始页面逻辑结束 ==========

// ========== 布局侧边栏逻辑 ==========
const DEFAULT_WIDTH = 188
const COLLAPSED_WIDTH = 80
const MIN_WIDTH = 80
const MAX_WIDTH = 188

const sidebarWidth = ref(getSidebarWidth() || DEFAULT_WIDTH)
const isCollapsed = ref(getSidebarCollapsed() || false)
const isDragging = ref(false)

watch(isCollapsed, (newVal) => {
    if (newVal) {
        sidebarWidth.value = COLLAPSED_WIDTH
    } else {
        sidebarWidth.value = DEFAULT_WIDTH
    }
    setSidebarCollapsed(newVal)
    setSidebarWidth(sidebarWidth.value)
})

interface SidebarItem {
    path: string
    name: string
    icon?: string
    menuOrder?: number
    children?: { path: string; name: string }[]
}

// 构建侧边栏树结构
const sidebarItems = computed<SidebarItem[]>(() => {
    const allRoutes = router.getRoutes()

    // 获取所有可显示的平面路由，并按 menuOrder 排序
    const displayRoutes = allRoutes.filter(r => {
        const path = r.path
        const isShow = r.meta?.show !== false
        return path.startsWith('/admin-home/') && path !== '/admin-home' && !path.includes(':') && isShow
    }).sort((a, b) => (Number(a.meta?.menuOrder) || 99) - (Number(b.meta?.menuOrder) || 99))

    // 获取父级分组路由
    const parentRoutes = displayRoutes.filter(r => r.children && r.children.length > 0)

    const items: SidebarItem[] = []

    // 1. 父级分组
    for (const parent of parentRoutes) {
        const children: { path: string; name: string }[] = []
        // 找到该分组的实际子路由，并按 menuOrder 排序
        const routeChildren = allRoutes
            .filter(r =>
                r.path.startsWith(parent.path + '/') &&
                r.path !== parent.path &&
                r.meta?.show !== false
            )
            .sort((a, b) => (Number(a.meta?.menuOrder) || 99) - (Number(b.meta?.menuOrder) || 99))
        for (const child of routeChildren) {
            children.push({
                path: child.path,
                name: (child.meta?.title as string) || (child.name as string)
            })
        }
        if (children.length > 0) {
            items.push({
                path: parent.path,
                name: (parent.meta?.title as string) || (parent.name as string),
                icon: parent.meta?.icon as string,
                menuOrder: Number(parent.meta?.menuOrder) || 99,
                children
            })
        }
    }

    // 2. 独立路由（不属于任何分组的叶子节点）
    const itemPaths = new Set(items.flatMap(i => [i.path, ...(i.children?.map(c => c.path) ?? [])]))
    const independentRoutes = displayRoutes.filter(r => !itemPaths.has(r.path))
    for (const route of independentRoutes) {
        items.push({
            path: route.path,
            name: (route.meta?.title as string) || (route.name as string),
            icon: route.meta?.icon as string,
            menuOrder: Number(route.meta?.menuOrder) || 99,
        })
    }

    // 按 menuOrder 升序排列，确保所有菜单项按正确顺序显示
    return items.sort((a, b) => (a.menuOrder || 99) - (b.menuOrder || 99))
})

// 展开状态
const expandedSet = reactive(new Set<string>())

// 默认展开当前路由所在的组
watch(() => route.path, (newPath) => {
    for (const item of sidebarItems.value) {
        if (item.children?.some(c => c.path === newPath)) {
            expandedSet.add(item.path)
        }
    }
}, { immediate: true })

const toggleExpand = (path: string) => {
    if (expandedSet.has(path)) {
        expandedSet.delete(path)
    } else {
        expandedSet.add(path)
    }
}

const currentRoute = computed(() => route.path)

// 面包屑 - 只显示当前路由
const breadcrumbList = computed(() => {
    return [{
        path: route.path,
        name: route.name as string,
        meta: route.meta
    }]
})

const toggleCollapse = () => {
    isCollapsed.value = !isCollapsed.value
}

const startDrag = (e: MouseEvent) => {
    e.preventDefault()
    isDragging.value = true
    document.body.style.userSelect = 'none'
    document.body.style.cursor = 'col-resize'

    const startX = e.clientX
    const startWidth = sidebarWidth.value

    const doDrag = (moveEvent: MouseEvent) => {
        if (!isDragging.value) return
        const deltaX = moveEvent.clientX - startX
        let newWidth = startWidth + deltaX
        if (newWidth < MIN_WIDTH) newWidth = MIN_WIDTH
        if (newWidth > MAX_WIDTH) newWidth = MAX_WIDTH
        sidebarWidth.value = newWidth
        if (newWidth <= MIN_WIDTH) {
            isCollapsed.value = true
        } else if (newWidth > MIN_WIDTH + 20) {
            isCollapsed.value = false
        }
    }

    const stopDrag = () => {
        if (!isDragging.value) return
        isDragging.value = false
        document.body.style.userSelect = ''
        document.body.style.cursor = ''
        setSidebarWidth(sidebarWidth.value)
        document.removeEventListener('mousemove', doDrag)
        document.removeEventListener('mouseup', stopDrag)
    }

    document.addEventListener('mousemove', doDrag)
    document.addEventListener('mouseup', stopDrag)
}

onMounted(async () => {
    const savedWidth = getSidebarWidth()
    if (savedWidth && savedWidth <= MIN_WIDTH) {
        isCollapsed.value = true
    }

    const token = getToken()
    if (!token) {
        router.push('/login')
        ElMessage.error(`身份认证异常`)
        return
    }
    await tokenAuth()

    const lastPath = localStorage.getItem('LAST_VISITED_PATH')
    if (route.path === '/admin-home') {
        const validLastPath = lastPath && lastPath !== '/' && lastPath.startsWith('/admin-home/')
        const redirectPath = validLastPath ? lastPath : '/admin-home/dashboards'
        router.push(redirectPath)
    }
})

onUnmounted(() => {
    document.body.style.userSelect = ''
    document.body.style.webkitUserSelect = ''
    document.body.style.cursor = ''
})
</script>

<style scoped lang="scss">
.nav-list {
    margin: 0;
    padding: 0;
    list-style: none;
    font-size: 14px;

    li {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 0 20px;
        height: 50px;
        font-size: 16px;
        line-height: 50px;
        cursor: pointer;
        color: #333;
        transition: all 0.2s ease;
        white-space: nowrap;
        overflow: hidden;

        &:hover {
            background-color: rgba(255, 255, 255, 0.1);
        }

        &.active {
            background-color: rgb(240, 240, 240);
            color: #333;
        }

        .el-icon {
            font-size: 20px;
            flex-shrink: 0;
            transition: margin 0.2s ease;
        }

        span {
            transition: opacity 0.2s ease;
        }
    }
}

// ====== 嵌套菜单分组样式 ======
.nav-group {
    display: block !important;
    padding: 0 !important;
    height: auto !important;
    line-height: normal !important;

    .nav-group-header {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 0 20px;
        height: 50px;
        line-height: 50px;
        cursor: pointer;
        color: #333;
        transition: all 0.2s ease;
        white-space: nowrap;
        overflow: hidden;

        &:hover {
            background-color: rgba(255, 255, 255, 0.1);
        }

        .el-icon {
            font-size: 20px;
            flex-shrink: 0;
            transition: margin 0.2s ease;
        }

        .expand-icon {
            margin-left: auto;
            font-size: 14px;
            transition: transform 0.25s ease;

            &.expanded {
                transform: rotate(180deg);
            }
        }
    }

    .nav-children {
        margin: 0;
        padding: 0;
        list-style: none;

        li {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 0 20px 0 50px;
            height: 42px;
            font-size: 14px;
            line-height: 42px;
            cursor: pointer;
            color: #555;
            transition: all 0.2s ease;
            white-space: nowrap;
            overflow: hidden;

            &:hover {
                background-color: rgba(255, 255, 255, 0.08);
            }

            &.active {
                background-color: rgb(240, 240, 240);
                color: #333;
                font-weight: 500;
            }

            span {
                transition: opacity 0.2s ease;
            }
        }
    }
}

.sidebar.collapsed {
    .nav-list li {
        justify-content: center;
        padding: 0 10px;

        .el-icon {
            margin: 0 10px;
        }

        span {
            opacity: 0;
            width: 0;
        }
    }

    .nav-group {
        .nav-group-header {
            justify-content: center;
            padding: 0 10px;

            .el-icon {
                margin: 0 10px;
            }

            span {
                opacity: 0;
                width: 0;
            }

            .expand-icon {
                display: none;
            }
        }

        .nav-children li {
            justify-content: center;
            padding: 0 10px;

            span {
                opacity: 0;
                width: 0;
            }
        }
    }

    .sidebar-header {
        justify-content: center;
        padding: 15px 10px;

        h2 {
            display: none;
        }

        img {
            margin: 0 auto;
            display: block;
        }
    }
}

.path-name {
    font-size: 26px;
    font-weight: 600;
    padding-inline: 4px;
}

.admin-layout {
    display: flex;
    height: 100vh;
    overflow: hidden;

    .sidebar {
        background-color: rgb(255, 255, 255);
        color: white;
        transition: width 0.2s ease;
        display: flex;
        flex-direction: column;
        flex-shrink: 0;
        border-right: 1px solid rgb(232, 232, 232);

        .sidebar-header {
            padding: 15px 20px;
            display: flex;
            justify-content: left;
            align-items: center;
            gap: 6px;
            background-color: rgb(255, 255, 255);
            border-bottom: 1px solid rgb(232, 232, 232);
            transition: all 0.2s ease;

            img {
                width: 30px;
                height: 30px;
                transition: all 0.2s ease;
            }

            h2 {
                margin: 0;
                font-size: 18px;
                color: #333;
                transition: opacity 0.2s ease;
            }
        }

        .sidebar-menu {
            flex: 1;
            overflow-y: auto;
        }
    }

    .drag-bar {
        width: 6px;
        background-color: #fafafa;
        cursor: col-resize;
        transition: background-color 0.2s;
        flex-shrink: 0;
        position: relative;
        z-index: 1000;

        &:hover {
            background-color: rgb(64, 158, 255);
        }

        &:active {
            background-color: rgb(64, 158, 255);
        }
    }

    .main-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        min-width: 0;

        .header {
            height: 60px;
            padding: 0 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-shrink: 0;

            .header-left {
                .el-breadcrumb {
                    font-size: 18px;
                }
            }

            .header-right {
                :deep(.custom-dropdown .el-dropdown-selfdefine),
                :deep(.custom-dropdown:focus .el-dropdown-selfdefine) {
                    outline: none !important;
                    border: none !important;
                    box-shadow: none !important;
                }

                :deep(.custom-dropdown:focus-visible),
                :deep(.custom-dropdown:hover) {
                    box-shadow: none !important;
                    outline: none !important;
                }

                .user-info {
                    display: flex;
                    align-items: center;
                    cursor: pointer;
                    padding: 8px 12px;
                    border-radius: 4px;
                    transition: background-color 0.2s;

                    &:hover {
                        background-color: #fff;
                        box-shadow: none !important;
                        outline: none !important;
                    }

                    .username {
                        margin: 0 8px;
                        font-size: 16px;
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    }
                }
            }
        }

        .content {
            flex: 1;
            padding: 10px 20px;
            overflow-y: auto;
            background-color: #fff;
        }
    }
}

.dialog-footer {
    text-align: right;
}
</style>

<style>
.avatar-uploader .avatar {
    width: 68px;
    height: 68px;
    display: block;
}

.avatar-uploader .el-upload {
    border: 1px dashed var(--el-border-color);
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
    border-color: var(--el-color-primary);
    box-shadow: none !important;
}

.el-icon.avatar-uploader-icon {
    font-size: 28px;
    color: #0f56b9;
    width: 178px;
    height: 178px;
    text-align: center;
}

/* 去除头像 hover 黑框阴影 */
.admin-layout .user-info .el-avatar:hover,
.admin-layout .el-dropdown .el-avatar:hover {
    box-shadow: none !important;
    outline: none !important;
}

.admin-layout .custom-dropdown:focus-visible,
.admin-layout .custom-dropdown:hover {
    box-shadow: none !important;
    outline: none !important;
}
</style>