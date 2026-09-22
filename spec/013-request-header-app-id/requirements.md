# 013 检测接口直接读取请求头调用方标识

## 需求

- 检测 Controller 不再调用 `AuthenticationContext.requirePrincipal()`。
- 同步、异步检测入口直接从 HTTP 请求头 `X-App-Id` 获取 `appId`，传给应用服务。
- 保留系统级鉴权过滤器与现有签名校验，不允许未通过鉴权的请求进入 Controller。
- 不向检测请求体新增 `appId` 字段，也不改变对外响应结构。
