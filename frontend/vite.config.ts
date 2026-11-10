import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Trong Docker, đặt API_PROXY_TARGET=http://backend:8080; ngoài Docker mặc định trỏ backend local.
const apiProxyTarget = process.env.API_PROXY_TARGET || 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Trình duyệt luôn gọi /api cùng origin (localhost:5173); cookie refresh HttpOnly
      // (Path=/api/auth) và Bearer access JWT chỉ được gửi tới backend qua proxy này.
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true,
      },
    },
  },
});
