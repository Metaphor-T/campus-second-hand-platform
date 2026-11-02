<template>
    <el-form class="product-filters" inline aria-label="商品筛选条件" @submit.prevent="apply">
        <el-form-item label="关键词"><el-input v-model="draft.title" maxlength="200" clearable placeholder="搜索商品标题" /></el-form-item>
        <el-form-item label="最低价"><el-input-number v-model="draft.minPrice" :min="0" :max="99999999.99" :precision="2" :controls="false" aria-label="最低价格" /></el-form-item>
        <el-form-item label="最高价"><el-input-number v-model="draft.maxPrice" :min="0" :max="99999999.99" :precision="2" :controls="false" aria-label="最高价格" /></el-form-item>
        <el-form-item label="新旧"><el-select v-model="draft.condition" clearable placeholder="不限"><el-option v-for="(text, index) in ['全新','几乎全新','有使用痕迹','老旧']" :key="text" :label="text" :value="index + 1" /></el-select></el-form-item>
        <el-form-item label="交易"><el-select v-model="draft.tradeType" clearable placeholder="不限"><el-option label="自提" :value="1" /><el-option label="邮寄" :value="2" /><el-option label="两者皆可" :value="3" /></el-select></el-form-item>
        <el-form-item label="排序"><el-select v-model="draft.sortBy"><el-option label="最新发布" value="latest" /><el-option label="价格从低到高" value="priceAsc" /><el-option label="价格从高到低" value="priceDesc" /><el-option label="收藏热度" value="hot" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" native-type="submit">应用筛选</el-button><el-button @click="reset">重置筛选</el-button></el-form-item>
        <p v-if="validationError" class="filter-error" role="alert">{{ validationError }}</p>
    </el-form>
</template>
<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { validateProductFilters, type ProductFilterValues } from '@/utils/product-experience'
const props = defineProps<{ modelValue: ProductFilterValues; keyword?: string }>()
const emit = defineEmits<{ apply: [filters: ProductFilterValues] }>()
const draft = reactive<ProductFilterValues>({ sortBy: 'latest', ...props.modelValue, title: props.keyword || '' })
const validationError = ref('')
watch(() => props.keyword, value => { draft.title = value || '' })
const apply = () => {
    validationError.value = validateProductFilters(draft)
    if (!validationError.value) emit('apply', { ...draft, title: draft.title?.trim() || undefined })
}
const reset = () => {
    Object.assign(draft, { title:'', minPrice:undefined, maxPrice:undefined, condition:undefined, tradeType:undefined, sortBy:'latest' })
    validationError.value = ''
    emit('apply', { ...draft })
}
</script>
<style scoped>
.product-filters { padding:16px; margin-bottom:20px; border:1px solid #d4d8df; border-radius:12px; background:#fff; }
.product-filters :deep(.el-input), .product-filters :deep(.el-select) { width:160px; }
.product-filters :deep(.el-input-number) { width:125px; }
.filter-error { color:#a61d24; font-size:16px; margin:0; }
@media(max-width:600px) { .product-filters :deep(.el-form-item) { display:flex; margin-right:0; } .product-filters :deep(.el-form-item__content) { flex:1; } .product-filters :deep(.el-input), .product-filters :deep(.el-select), .product-filters :deep(.el-input-number) { width:100%; } }
</style>
