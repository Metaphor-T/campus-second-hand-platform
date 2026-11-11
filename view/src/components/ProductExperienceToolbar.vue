<template>
    <!-- 仅保留商品页共用的无障碍样式，不渲染说明条。 -->
</template>
<script setup lang="ts">
defineProps<{ guest?: boolean }>()
</script>
<style lang="scss">
.product-module :is(button, a, [role="button"]):focus-visible { outline:3px solid #075eac; outline-offset:3px; }
.product-module .filter-item, .product-module .cnp-item, .product-module .cnp-all { min-height:44px; }
.product-module .product-empty, .product-module .product-request-error { margin:16px 0; padding:18px; color:#374151; }
.product-module .product-request-error { border:1px solid #b45309; border-radius:10px; background:#fffbeb; }
.product-module .product-request-error button { margin-left:12px; }
.product-module .image-fallback { display:flex; align-items:center; justify-content:center; height:100%; color:#374151; background:#f3f4f6; font-size: 1rem; }
@media (prefers-reduced-motion:reduce) { .product-module *, .product-module *::before, .product-module *::after { animation:none !important; transition:none !important; scroll-behavior:auto !important; } }
</style>
