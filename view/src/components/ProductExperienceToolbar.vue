<template>
    <div class="product-experience-toolbar">
        <p>{{ guest ? '无需注册即可浏览、搜索和查看商品；收藏、发布和交易时再登录。' : '收藏、想要和下单等交互记录用于热门统计与个性化推荐；商品图片用于展示闲置信息。' }}</p>
    </div>
</template>
<script setup lang="ts">
defineProps<{ guest?: boolean }>()
</script>
<style lang="scss">
.product-experience-toolbar { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; margin-bottom:16px; padding:12px 16px; background:#f4f6f8; border-radius:10px; color:#374151; font-size: 1.071429rem; line-height:1.6; }
.product-experience-toolbar p { margin:0; flex:1; min-width:220px; }
.product-module :is(button, a, [role="button"]):focus-visible { outline:3px solid #075eac; outline-offset:3px; }
.product-module .filter-item, .product-module .cnp-item, .product-module .cnp-all { min-height:44px; }
.product-module .product-empty, .product-module .product-request-error { margin:16px 0; padding:18px; color:#374151; }
.product-module .product-request-error { border:1px solid #b45309; border-radius:10px; background:#fffbeb; }
.product-module .product-request-error button { margin-left:12px; }
.product-module .image-fallback { display:flex; align-items:center; justify-content:center; height:100%; color:#374151; background:#f3f4f6; font-size: 1rem; }
@media (prefers-reduced-motion:reduce) { .product-module *, .product-module *::before, .product-module *::after { animation:none !important; transition:none !important; scroll-behavior:auto !important; } }
</style>
