import { computed, readonly, ref } from 'vue'

export const FONT_SCALES = [1, 1.25, 1.5, 2] as const
export const FONT_SCALE_KEY = 'campus-font-scale'
export function readFontScale(storage: Pick<Storage, 'getItem'>): number {
    try {
        const saved = storage.getItem(FONT_SCALE_KEY)
        if (saved !== null) return FONT_SCALES.includes(Number(saved) as typeof FONT_SCALES[number]) ? Number(saved) : 1
        return storage.getItem('product-large-text') === 'true' ? 1.25 : 1
    } catch { return 1 }
}
const scale = ref(typeof localStorage === 'undefined' ? 1 : readFontScale(localStorage))
export const fontScale = readonly(scale)
export const isLargeText = computed(() => scale.value > 1)
export function setFontScale(value: number) {
    if (!FONT_SCALES.includes(value as typeof FONT_SCALES[number])) return
    scale.value = value
    if (typeof document !== 'undefined') {
        document.documentElement.style.setProperty('--font-scale', String(value))
        document.documentElement.dataset.fontScale = String(value)
    }
    try { localStorage.setItem(FONT_SCALE_KEY, String(value)) } catch { /* 无存储权限仍可使用 */ }
}
export function initializeAccessibility() {
    setFontScale(scale.value)
    const onStorage = (event: StorageEvent) => {
        if (event.key === FONT_SCALE_KEY || event.key === null) {
            let value = 1
            try { value = readFontScale(localStorage) } catch { /* 降级默认字号 */ }
            setFontScale(value)
        }
    }
    window.addEventListener('storage', onStorage)
}
