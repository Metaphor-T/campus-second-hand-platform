<template>
    <div class="error-page">
        <div class="error-content">
            <h1 class="error-code">404</h1>
            <p class="error-title">页面不存在</p>
            <p class="error-desc">抱歉，您访问的页面不存在或已被移除</p>
            <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
        </div>
    </div>
</template>

<script setup lang="ts">
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
    color: #409eff;
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
}
</style>
