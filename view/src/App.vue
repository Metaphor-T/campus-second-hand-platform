<script setup lang="ts">
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import AccessibilitySettings from './components/AccessibilitySettings.vue'
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<template>
  <el-config-provider :locale="zhCn">
    <a class="skip-link" href="#main-content">跳到主要内容</a>
    <div v-if="!/^\/(login|register|user-home|guest-home|admin-home)(\/|$)/.test(route.path)" class="reading-fallback"><AccessibilitySettings /></div>
    <div id="main-content" tabindex="-1"><router-view /></div>
  </el-config-provider>
</template>
