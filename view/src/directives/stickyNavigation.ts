import type { ObjectDirective } from 'vue'

const observers = new WeakMap<HTMLElement, ResizeObserver>()
export const stickyNavigation: ObjectDirective<HTMLElement> = {
    mounted(element) {
        const measure = () => document.documentElement.style.setProperty('--site-nav-height', `${element.getBoundingClientRect().height}px`)
        const observer = new ResizeObserver(measure)
        observer.observe(element)
        observers.set(element, observer)
        measure()
    },
    unmounted(element) {
        observers.get(element)?.disconnect()
        observers.delete(element)
        document.documentElement.style.removeProperty('--site-nav-height')
    },
}
