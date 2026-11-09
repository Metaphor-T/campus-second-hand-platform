<script setup lang="ts">
import { ref, useId } from 'vue'
import { fontScale, setFontScale } from '@/utils/accessibility'
defineEmits<{ close: [] }>()
const group = useId()
const announcement = ref('')
const options = [{ value: 1, label: '标准' }, { value: 1.25, label: '大字' }, { value: 1.5, label: '特大字' }, { value: 2, label: '超大字' }]
function changeScale(value: number) {
  setFontScale(value)
  announcement.value = `字号已调整为 ${value * 100}%`
}
</script>
<template>
  <p class="reading-current">当前字号 <strong>{{ fontScale * 100 }}%</strong> · 全站生效</p>
  <fieldset class="reading-options">
    <legend>选择适合你的字号</legend>
    <label v-for="option in options" :key="option.value" :class="{ selected: fontScale === option.value }">
      <input type="radio" :name="group" class="font-scale-option" :value="option.value" :checked="fontScale === option.value" @change="changeScale(option.value)">
      <span>{{ option.label }}</span><span>{{ option.value * 100 }}%</span>
    </label>
  </fieldset>
  <p class="reading-preview">校园好物，清晰阅读。<br>设置会保留，登录和切换页面后继续生效。</p>
  <div class="reading-actions"><button type="button" @click="changeScale(1)">恢复默认</button><button type="button" @click="$emit('close')">完成</button></div>
  <span class="sr-only" role="status" aria-live="polite">{{ announcement }}</span>
</template>
