import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      // 样式已全局引入 element-plus/dist/index.css，关闭按需样式注入，
      // 避免 unplugin 编译期注入 element-plus/es/components/*/style/css 子路径依赖，
      // 从源头消除「运行时发现新依赖 → 重新预构建 → 整页刷新」
      resolvers: [ElementPlusResolver({ importStyle: false })],
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: false })],
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  // 显式预构建懒加载页面独有依赖，避免首次点击菜单时触发 Vite 运行时依赖发现 + full-reload
  optimizeDeps: {
    include: [
      // unplugin 对 el-* 组件/API 实际注入的导入路径是 element-plus/es（非根入口 element-plus）
      'element-plus/es',
      '@element-plus/icons-vue',
      'axios',   // api 层依赖，仅经懒加载页面可达
      'echarts', // 仅 admin/Dashboards.vue 使用
      'vue',
      'vue-router',
    ],
  },
  server: {
    port: 21091,        // 使用的端口号
    open: true,        // 自动打开浏览器
    host: '0.0.0.0',    // 允许所有网络接口访问
    hmr: {
      host: '0.0.0.0',  // HMR也允许外部访问
    },
    cors: true,
  }
})