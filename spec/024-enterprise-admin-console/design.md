# 设计方案

## 工程边界

前端目录为 `study-console-web`，通过 HTTP 调用 `admin-service`。开发环境由 Vite 将 `/api` 代理到 `http://localhost:8081`，生产环境由 Nginx 代理到可配置的后端服务地址。

## 分层

```text
views / layouts / components
            |
router / stores / locales
            |
typed admin api contract
        /          \
HTTP implementation  Mock implementation
```

页面不直接调用 Axios。所有后端访问都通过类型化的 `AdminApi` 契约，便于测试、Mock 和后续接口版本演进。

## 权限模型

路由通过 `meta.permission` 声明权限编码。全局路由守卫首先恢复登录身份，再判断当前用户是否包含页面权限。按钮操作继续使用相同权限编码控制显示，但后端仍是最终授权边界。

## 页面结构

- 工作台：关键指标、服务状态、最近规则发布和审计事件。
- 规则管理：规则集选择、草稿规则、校验、发布和发布记录。
- 配置管理：命名空间筛选、配置编辑、版本和启停状态。
- 用户权限：用户、角色、权限集合和租户隔离信息。
- 日志中心：按 taskId、结果、类型筛选并查看责任链时间线。

## 视觉原则

采用浅色工作区、深色窄侧栏和青绿色主操作色。页面以表格、筛选栏和抽屉为主，避免营销式大标题和装饰性卡片。状态色同时使用文字和图标表达，避免只依赖颜色。
