<template>
    <el-form class="product-filters" label-position="top" aria-label="商品筛选条件" @submit.prevent="apply">
        <div class="filter-grid">
            <el-form-item class="filter-keyword" label="关键词">
                <el-input v-model="draft.title" maxlength="200" clearable placeholder="搜索商品标题" />
            </el-form-item>
            <el-form-item class="filter-price" label="价格区间（元）">
                <div class="price-range">
                    <el-input-number v-model="draft.minPrice" :min="0" :max="99999999.99" :precision="2" :controls="false" placeholder="最低价" aria-label="最低价格" :aria-invalid="!!validationError" :aria-describedby="validationError ? priceErrorId : undefined" />
                    <span class="price-separator" aria-hidden="true">—</span>
                    <el-input-number v-model="draft.maxPrice" :min="0" :max="99999999.99" :precision="2" :controls="false" placeholder="最高价" aria-label="最高价格" :aria-invalid="!!validationError" :aria-describedby="validationError ? priceErrorId : undefined" />
                </div>
            </el-form-item>
            <el-form-item label="新旧程度">
                <el-select v-model="draft.condition" clearable placeholder="不限"><el-option v-for="(text, index) in ['全新','几乎全新','有使用痕迹','老旧']" :key="text" :label="text" :value="index + 1" /></el-select>
            </el-form-item>
            <el-form-item label="交易方式">
                <el-select v-model="draft.tradeType" clearable placeholder="不限"><el-option label="自提" :value="1" /><el-option label="邮寄" :value="2" /><el-option label="两者皆可" :value="3" /></el-select>
            </el-form-item>
            <el-form-item label="排序方式">
                <el-select v-model="draft.sortBy"><el-option label="最新发布" value="latest" /><el-option label="价格从低到高" value="priceAsc" /><el-option label="价格从高到低" value="priceDesc" /><el-option label="收藏热度" value="hot" /></el-select>
            </el-form-item>
        </div>
        <p v-if="validationError" :id="priceErrorId" class="filter-error" role="alert">{{ validationError }}</p>
        <div class="filter-actions">
            <el-button native-type="button" @click="reset">重置筛选</el-button>
            <el-button type="primary" native-type="submit">应用筛选</el-button>
        </div>
    </el-form>
</template>
<script setup lang="ts">
import { reactive, ref, useId, watch } from 'vue'
import { validateProductFilters, type ProductFilterValues } from '@/utils/product-experience'
const props = defineProps<{ modelValue: ProductFilterValues; keyword?: string }>()
const emit = defineEmits<{ apply: [filters: ProductFilterValues] }>()
const draft = reactive<ProductFilterValues>({ sortBy: 'latest', ...props.modelValue, title: props.keyword || '' })
const validationError = ref('')
const priceErrorId = `filter-price-error-${useId()}`
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
.product-filters { --filter-control-height:max(44px,3rem); container-type:inline-size; padding:24px; margin-bottom:20px; border:1px solid #d4d8df; border-radius:12px; background:#fff; }
.filter-grid { display:grid; grid-template-columns:repeat(6,minmax(0,1fr)); gap:20px 24px; }
.filter-grid :deep(.el-form-item) { grid-column:span 2; display:flex; flex-direction:column; align-items:stretch; min-width:0; margin:0; }
.filter-grid :deep(.filter-keyword), .filter-grid :deep(.filter-price) { grid-column:span 3; }
.filter-grid :deep(.el-form-item__label) { display:block; min-height:0; height:auto; width:auto !important; margin:0 0 8px; padding:0; line-height:1.5; text-align:left; color:#41454b; }
.filter-grid :deep(.el-form-item__content) { display:block; width:100%; min-width:0; margin-left:0 !important; }
.filter-grid :deep(.el-input), .filter-grid :deep(.el-select), .filter-grid :deep(.el-input-number) { width:100%; min-width:0; }
.filter-grid :deep(.el-input-number) { min-height:var(--filter-control-height); height:auto; }
.filter-grid :deep(.el-input__wrapper), .filter-grid :deep(.el-select__wrapper) { min-height:var(--filter-control-height); height:auto; }
/* 放大外框时同步放大内部行框，避免组件默认 24px 行高裁切汉字。 */
.filter-grid :deep(.el-select__wrapper) { font-size:1rem; line-height:1.5; padding-block:8px; }
.filter-grid :deep(.el-select__selection) { min-height:1.5em; }
.filter-grid :deep(.el-select__placeholder), .filter-grid :deep(.el-select__selected-item) { line-height:1.5; }
.filter-grid :deep(.el-select__input) { height:auto; min-height:1.5em; line-height:1.5; }
.filter-grid :deep(.el-input-number .el-input__inner) { text-align:left; }
.price-range { display:grid; grid-template-columns:minmax(0,1fr) auto minmax(0,1fr); align-items:center; gap:12px; width:100%; min-width:0; }
.price-separator { color:#6b7078; }
.filter-actions { display:flex; justify-content:flex-end; flex-wrap:wrap; gap:12px; margin-top:24px; padding-top:20px; border-top:1px solid #eceef1; }
.filter-actions :deep(.el-button) { min-height:var(--filter-control-height); margin:0; }
.filter-error { color:#a61d24; font-size:1rem; line-height:1.6; margin:16px 0 0; }
@container (max-width:48rem) { .filter-grid :deep(.filter-keyword), .filter-grid :deep(.filter-price) { grid-column:1 / -1; } }
@container (max-width:32rem) { .filter-grid :deep(.el-form-item) { grid-column:1 / -1; } }
@container (max-width:20rem) { .price-range { grid-template-columns:minmax(0,1fr); } .price-separator { display:none; } }
@media(max-width:600px) { .product-filters { padding:16px; } .filter-actions :deep(.el-button) { flex:1 1 8rem; } }
</style>
