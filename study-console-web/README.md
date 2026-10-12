# ContentGuard 风控管理台

独立的内容风控运营管理前端，对接项目中的 `admin-service`。当前包含登录、权限路由、工作台、SpEL 规则、配置项、用户角色和检测日志页面。

## 技术栈

- Vue 3 + TypeScript + Vite
- Vue Router + Pinia + Vue I18n
- Element Plus + Lucide Icons
- Axios
- Vitest + Vue Test Utils
- ESLint + Prettier

## 本地启动

```powershell
cd study-console-web
npm install
npm run dev
```

默认开发环境启用 Mock 数据，访问 `http://localhost:5173`，登录信息为：

```text
租户：default
账号：admin
密码：admin123
```

## 连接真实管理服务

复制 `.env.example` 为 `.env.local`，设置：

```dotenv
VITE_API_BASE_URL=/api
VITE_API_PROXY_TARGET=http://localhost:8081
VITE_ENABLE_MOCK=false
VITE_APP_ENV=local
```

Vite 会把 `/api` 转发到 `admin-service`。真实环境需要先准备 MySQL，并为管理服务设置 `ADMIN_BOOTSTRAP_PASSWORD`。

## 工程验证

```powershell
npm run type-check
npm run lint
npm run format:check
npm run test
npm run build
```

## Docker

```powershell
docker build -t content-risk/study-console-web:local .
docker run --rm -p 8088:80 content-risk/study-console-web:local
```

容器中的 Nginx 默认将 `/api` 代理到 `admin-service:8081`，适合部署到与管理服务相同的 Kubernetes 命名空间。

## 目录

```text
src/
├── api/          # API 契约、HTTP 实现和 Mock 实现
├── components/   # 通用展示组件
├── layouts/      # 管理台壳层
├── locales/      # 中英文资源
├── router/       # 路由和权限守卫
├── stores/       # 登录态和应用状态
├── styles/       # 全局设计规范
├── types/        # 后端接口类型
├── utils/        # 无状态工具
└── views/        # 业务页面
```
