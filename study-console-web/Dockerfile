ARG NODE_IMAGE=node:24-alpine
ARG NGINX_IMAGE=nginx:1.29-alpine
FROM ${NODE_IMAGE} AS build

WORKDIR /workspace
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_APP_ENV=production
ENV VITE_API_BASE_URL=/api \
    VITE_ENABLE_MOCK=false \
    VITE_APP_ENV=${VITE_APP_ENV}
RUN npm run build

FROM ${NGINX_IMAGE}

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1/healthz || exit 1
