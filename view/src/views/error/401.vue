<template>
    <div class="error-page">
        <div class="error-content">
            <h1 class="error-code">401</h1>
            <p class="error-title">未授权访问</p>
            <p class="error-desc">抱歉，您没有权限访问此页面，请联系管理员或使用授权账号登录</p>
            <div class="btn-group">
                <el-button type="primary" @click="$router.push('/login')">返回登录</el-button>
                <el-button @click="$router.back()">返回上一页</el-button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { clearToken, clearRole } from '@/utils/localStorage'

onMounted(() => {
    // 进入 401 页面时自动清除无效 token 和 role
    clearToken()
    clearRole()
})
</script>

<style scoped>
.error-page {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background-color: #f5f7fa;
}

.error-content {
    text-align: center;
}

.error-code {
    font-size: 120px;
    font-weight: 700;
    color: #e6a23c;
    margin: 0;
    line-height: 1;
}

.error-title {
    font-size: 24px;
    color: #333;
    margin: 16px 0 8px;
}

.error-desc {
    font-size: 14px;
    color: #999;
    margin-bottom: 32px;
    max-width: 360px;
    line-height: 1.6;
}

.btn-group {
    display: flex;
    gap: 12px;
    justify-content: center;
}
</style>
