# 需求说明

## 背景

当前检测接口在 Controller 中直接调用固定的 HMAC-SHA256 验签逻辑。随着管理台、内部服务、开放接口和健康检查等端点增加，不同 URL 需要使用不同鉴权模式，并且鉴权规则需要由管理台统一维护。

## 目标

- 支持按照 HTTP Method 和 URL 路径匹配鉴权规则。
- 支持 `SIGNATURE`、`TOKEN`、`INTERNAL`、`ANONYMOUS` 等可扩展鉴权模式。
- 鉴权规则最终可由管理台创建、校验、发布、回滚和审计。
- 业务服务只使用已发布的规则快照，不直接依赖管理台在线状态。
- 鉴权配置异常、缺失或未匹配时默认拒绝请求，避免鉴权降级。
- 当前阶段实现系统级 HTTP 鉴权，管理台完成后复用发布协议动态维护规则。

## 使用场景

```text
POST /api/detect/sync             -> SIGNATURE
POST /api/detect/async            -> SIGNATURE
GET  /api/detect/tasks/{taskId}   -> SIGNATURE
/admin/**                         -> TOKEN
/internal/**                      -> INTERNAL
/actuator/health                  -> ANONYMOUS
```

## 本期范围

- 在 Servlet Filter 中完成鉴权，不允许 Controller 绕过或自行选择鉴权模式。
- 实现 `SIGNATURE` 和 `ANONYMOUS`，预留其他 Provider 扩展点。
- 使用数据库 SDK 读取完整规则快照，并在失败时保留上一有效快照。
- 使用进程内 nonce 防重放；多实例部署前替换为 Redis 实现。
- 凭证从环境变量注入；后续可替换为数据库加密存储或 STS。

## 非目标

- 本需求不实现管理台页面和配置接口。
- 本需求不实现 JWT、mTLS、OAuth2 或完整 STS。
- 本需求不支持请求方通过请求头自行决定鉴权模式。

## 验收标准

- 明确 URL 规则模型、匹配优先级和冲突处理方式。
- 明确管理台发布、服务端加载、失败回退和回滚流程。
- 明确扩展新鉴权模式时的接口边界。
- 明确安全默认值、审计要求和后续实施阶段。
- 检测 Controller 不再包含手工验签代码。
- 请求体被纳入签名且经过过滤器后仍可被 Controller 正常读取。
- 同一个 `appId + nonce` 在有效期内只能认证成功一次。
