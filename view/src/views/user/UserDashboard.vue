<template>
    <div class="user-dashboard">
        <!-- 欢迎卡片 -->
        <div class="welcome-card">
            <div class="welcome-left">
                <el-avatar :size="64" v-if="user.avatar" :src="getImageUrl(user.avatar)" />
                <el-avatar :size="64" v-else>{{ (user?.username ?? '用')[0] }}</el-avatar>
                <div class="welcome-text">
                    <h2>你好，{{ user?.username ?? '用户' }}</h2>
                    <p>欢迎回到校园淘物校园管理系统</p>
                </div>
            </div>
        </div>

        <!-- 个人信息卡片 -->
        <div class="info-grid">
            <div class="info-card">
                <div class="info-icon account-icon">
                    <el-icon :size="22"><User /></el-icon>
                </div>
                <div class="info-body">
                    <span class="info-label">账号</span>
                    <span class="info-value">{{ user?.account ?? '-' }}</span>
                </div>
            </div>

            <div class="info-card">
                <div class="info-icon phone-icon">
                    <el-icon :size="22"><Phone /></el-icon>
                </div>
                <div class="info-body">
                    <span class="info-label">手机号</span>
                    <span class="info-value">{{ user?.phone || '未设置' }}</span>
                </div>
            </div>

            <div class="info-card">
                <div class="info-icon gender-icon">
                    <el-icon :size="22"><Male v-if="user?.gender === 1" /><Female v-else /></el-icon>
                </div>
                <div class="info-body">
                    <span class="info-label">性别</span>
                    <span class="info-value">{{ user?.gender === 1 ? '男' : user?.gender === 2 ? '女' : '未设置' }}</span>
                </div>
            </div>

            <div class="info-card">
                <div class="info-icon status-icon">
                    <el-icon :size="22"><Check v-if="user?.loginStatus === 1" /><Close v-else /></el-icon>
                </div>
                <div class="info-body">
                    <span class="info-label">账号状态</span>
                    <span class="info-value" :class="{ 'status-normal': user?.loginStatus === 1, 'status-banned': user?.loginStatus !== 1 }">
                        {{ user?.loginStatus === 1 ? '正常' : '已封禁' }}
                    </span>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { User, Phone, Male, Female, Check, Close } from '@element-plus/icons-vue'
import { userTokenAuthApi } from '@/api/user-api'
import type { BaseUserVO } from '@/types/user'

const user = ref<BaseUserVO>({})

const getImageUrl = (filePath: string) => {
    if (!filePath) return ''
    if (filePath.startsWith('http://') || filePath.startsWith('https://')) return filePath
    return `${import.meta.env.VITE_API_BASE_URL}${filePath}`
}

onMounted(async () => {
    try {
        const response = await userTokenAuthApi()
        user.value = response?.data ?? {}
    } catch {
        // 父级 UserHome 已处理 token 异常
    }
})
</script>

<style scoped lang="scss">
@use '../../styles/variables' as *;
.user-dashboard {
    display: flex;
    flex-direction: column;
    gap: $spacing-lg;
}

// ====== 欢迎卡片（品牌深蓝渐变） ======
.welcome-card {
    background: linear-gradient(135deg, $color-primary 0%, $color-primary-light 100%);
    border-radius: $radius-lg;
    padding: $spacing-xl $spacing-2xl;
    color: $color-text-white;
    position: relative;
    overflow: hidden;

    // 装饰圆
    &::before {
        content: '';
        position: absolute;
        top: -60px;
        right: -40px;
        width: 200px;
        height: 200px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.04);
    }

    &::after {
        content: '';
        position: absolute;
        bottom: -80px;
        right: 60px;
        width: 160px;
        height: 160px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.03);
    }

    .welcome-left {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: center;
        gap: 20px;

        :deep(.el-avatar) {
            border: 3px solid rgba(255, 255, 255, 0.35);
            background: rgba(255, 255, 255, 0.12);
            color: #fff;
            font-size: 24px;
        }
    }

    .welcome-text {
        h2 {
            margin: 0 0 4px 0;
            font-size: $font-size-xl;
            font-weight: 700;
            letter-spacing: 0.3px;
        }

        p {
            margin: 0;
            font-size: $font-size-sm;
            opacity: 0.8;
        }
    }
}

// ====== 信息格子 ======
.info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: $spacing-md;
}

.info-card {
    display: flex;
    align-items: center;
    gap: $spacing-md;
    background: $color-surface;
    border-radius: $card-radius;
    padding: $spacing-lg $spacing-xl;
    box-shadow: $shadow-sm;
    transition: box-shadow 0.25s, transform 0.25s;

    &:hover {
        box-shadow: $shadow-md;
        transform: translateY(-2px);
    }

    .info-icon {
        width: 46px;
        height: 46px;
        border-radius: $radius-md;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        &.account-icon { background: rgba($color-primary, 0.08); color: $color-primary; }
        &.phone-icon  { background: rgba(#34c759, 0.1); color: #34c759; }
        &.gender-icon { background: rgba($color-accent, 0.12); color: $color-accent; }
        &.status-icon { background: rgba(#ff3b30, 0.08); color: #ff3b30; }
    }

    .info-body {
        display: flex;
        flex-direction: column;
        gap: 3px;

        .info-label {
            font-size: $font-size-xs;
            color: $color-text-secondary;
            font-weight: 500;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .info-value {
            font-size: $font-size-md;
            color: $color-text;
            font-weight: 600;

            &.status-normal { color: #34c759; }
            &.status-banned { color: #ff3b30; }
        }
    }
}
</style>