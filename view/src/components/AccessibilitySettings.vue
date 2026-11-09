<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId, watch } from 'vue'
import { fontScale } from '@/utils/accessibility'
import ReadingOptions from './ReadingOptions.vue'
defineProps<{ appearance?: 'glass' }>()
const opened = ref(false)
const narrow = ref(false)
const trigger = ref<HTMLButtonElement>()
const dialogMode = computed(() => narrow.value || fontScale.value >= 1.5)
const panelId = `reading-${useId()}`
let media: MediaQueryList
const resize = () => { narrow.value = media.matches }
function restoreFocus() { trigger.value?.focus({ preventScroll: true }) }
function close() { opened.value = false; nextTick(restoreFocus) }
async function focusOption() {
  await nextTick()
  document.querySelector<HTMLInputElement>(`#${CSS.escape(panelId)} input:checked`)?.focus()
}
function outside(event: PointerEvent) {
  if (dialogMode.value || !opened.value) return
  const target = event.target as Node
  if (!trigger.value?.contains(target) && !document.getElementById(panelId)?.contains(target)) opened.value = false
}
function trapKeys(event: KeyboardEvent) {
  if (event.key === 'Escape') { event.stopPropagation(); close(); return }
  if (event.key !== 'Tab' || dialogMode.value) return
  const items = [...(document.getElementById(panelId)?.querySelectorAll<HTMLElement>('input:checked, button') ?? [])]
  const first = items[0], last = items[items.length - 1]
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
}
watch(dialogMode, () => { if (opened.value) focusOption() })
onMounted(() => {
  media = window.matchMedia('(max-width: 640px)')
  resize()
  media.addEventListener('change', resize)
  document.addEventListener('pointerdown', outside)
})
onBeforeUnmount(() => {
  media?.removeEventListener('change', resize)
  document.removeEventListener('pointerdown', outside)
})
</script>
<template>
  <div class="reading-settings" :class="{ 'reading-settings--glass': appearance === 'glass' }">
    <el-popover v-if="!dialogMode" trigger="click" :visible="opened" placement="bottom-start" :width="380" popper-class="reading-popover" :persistent="false" @after-enter="focusOption">
      <template #reference>
        <span class="reading-anchor">
        <button ref="trigger" class="accessibility-toggle" type="button" :aria-expanded="opened" :aria-controls="opened ? panelId : undefined" aria-haspopup="dialog" @click="opened ? close() : opened = true">
          <span class="reading-icon" aria-hidden="true">Aa</span><span>阅读设置·字号</span>
        </button>
        </span>
      </template>
      <div v-if="!dialogMode" :id="panelId" class="reading-panel" role="dialog" aria-label="阅读设置·字号" @keydown="trapKeys"><ReadingOptions @close="close" /></div>
    </el-popover>
    <button v-else ref="trigger" class="accessibility-toggle" type="button" :aria-expanded="opened" :aria-controls="opened ? panelId : undefined" aria-haspopup="dialog" @click="opened ? close() : opened = true">
      <span class="reading-icon" aria-hidden="true">Aa</span><span>阅读设置·字号</span>
    </button>
    <el-dialog :model-value="opened && dialogMode" title="阅读设置·字号" width="480px" align-center append-to-body destroy-on-close class="reading-dialog" @update:model-value="close" @opened="focusOption" @closed="restoreFocus">
      <div v-if="dialogMode" :id="panelId" class="reading-panel" @keydown="trapKeys"><ReadingOptions @close="close" /></div>
    </el-dialog>
  </div>
</template>
