# Frontend dev container — Vite dev server (HMR) ở cổng 5173.
# Node 24 thỏa `engines` của Vite 8 / @vitejs/plugin-react (^20.19.0 || >=22.12.0).
# Build production không chạy trong image này; xem ARCHITECTURE.md §11.
FROM node:24-alpine
WORKDIR /app

# Cài phụ thuộc ở layer riêng: chỉ chạy lại npm ci khi package.json/package-lock.json đổi.
# npm ci cần package-lock.json (orchestrator sinh sau khi cài) — thiếu lockfile thì build dừng, đúng chủ ý.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

EXPOSE 5173

# Vite phải lắng nghe ngoài container; proxy /api → API_PROXY_TARGET do vite.config.ts đọc.
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
