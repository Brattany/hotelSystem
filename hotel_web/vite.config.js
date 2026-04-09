import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 设置 @ 符号指向 src 目录
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000, // 前端运行在 3000 端口
    proxy: {
      // 代理配置：将所有 /api 开头的请求转发到 SpringBoot
      '/api': {
        target: 'http://localhost:8080', 
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})