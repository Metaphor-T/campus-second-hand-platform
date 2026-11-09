import { createApp } from 'vue'
import './styles/global.scss'
import App from './App.vue'
import router from './router'
import 'element-plus/dist/index.css'
import './styles/accessibility.scss'
import { initializeAccessibility } from './utils/accessibility'
import { accessiblePagination } from './directives/accessiblePagination'
import { stickyNavigation } from './directives/stickyNavigation'

initializeAccessibility()
createApp(App).directive('accessible-pagination', accessiblePagination).directive('sticky-navigation', stickyNavigation).use(router).mount('#app')
