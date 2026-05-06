import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  base: '/hotpulse/',
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
      // Socket.io-client 使用 /socket.io 作为传输路径，代理至 netty-socketio 服务器（端口 9001）
      '/socket.io': {
        target: 'http://localhost:9001',
        changeOrigin: true,
        ws: true,
      },
    },
  },
})
