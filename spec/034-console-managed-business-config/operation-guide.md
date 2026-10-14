# 操作说明

## 当前预览

前端开发服务运行在 `http://127.0.0.1:5173`，默认读取 `study-console-web/.env.development`，其中 `VITE_ENABLE_MOCK=true`。页面上的 `MOCK` 标识表示操作只写入浏览器当前会话的模拟数据，刷新后不保留，也不写 MySQL。

## 连接真实管理服务

1. 确保本机 MySQL 的 `content_risk` 已执行至 V9；根目录 `application-local.yaml` 可连接该库。
2. 启动 `admin-service`，激活 `local` profile。首次启动时按现有引导配置设置 `ADMIN_BOOTSTRAP_PASSWORD`；请使用独立的管理密码。
3. 将 `study-console-web/.env.development` 中 `VITE_ENABLE_MOCK` 改为 `false`，重启 Vite 开发服务。已有 `VITE_API_PROXY_TARGET=http://localhost:8081` 会代理管理接口。
4. 登录具有 `CONFIG_READ` 与 `CONFIG_WRITE` 权限的租户用户，再打开“接入与规则”或“媒资管理”。

配置保存成功后可在对应 MySQL 表中核对 `tenant_id`、`created_by`、`updated_by`、`version`。当前检测运行链路尚未读取这些配置，不能用在线测试结果判断新规则是否生效。
