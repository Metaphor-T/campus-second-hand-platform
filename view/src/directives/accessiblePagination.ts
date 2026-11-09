import type { ObjectDirective } from 'vue'
// Element Plus 的页码跳转输入框未提供名称，限定在分页组件中补齐。
function labelJumpInput(element: HTMLElement) {
    element.querySelectorAll<HTMLInputElement>('.el-pagination__sizes input').forEach(input => {
        input.setAttribute('aria-label', '每页显示条数')
    })
    element.querySelectorAll<HTMLInputElement>('.el-pagination__jump input').forEach(input => {
        input.setAttribute('aria-label', '跳转到页码')
    })
}
export const accessiblePagination: ObjectDirective<HTMLElement> = {
    mounted: labelJumpInput,
    updated: labelJumpInput,
}
