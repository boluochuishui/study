# 设计说明

## 模块边界

`base-sdk` 输出 `auth` 包：规则匹配、HMAC 验签、Provider/凭证/nonce 扩展点、认证上下文、日志记录和 Servlet 过滤器。

`access-service` 输出 `access.auth`：`DatabaseAuthConfigSource` 读取 `config-data` 发布快照，`AccessAuthConfiguration` 提供检测接口和探针的引导规则。SDK 只依赖 `AuthConfigSource` 和 `AuthConfigSnapshot`，不关心数据来源和具体 URL。

## 装配方式

- `AuthAutoConfiguration` 通过 Spring Boot 自动配置导入。
- 仅 Servlet 服务且 `study.auth.enabled=true` 时启用。
- 使用 `@ConditionalOnMissingBean` 允许调用方替换凭证源、nonce 存储、匹配器或 Provider。
- 未提供 `LogRecorder` 时使用 SDK 的 SLF4J 日志实现。
- `study.auth.skip-paths` 和 `study.auth.json-only-paths` 由服务配置，过滤器不内置检测服务路由。
- 接入服务继续通过数据库拉取鉴权快照；初始化使用本服务引导规则，刷新失败保留上一份有效快照。

## 边界说明

默认环境凭证源和内存 nonce 存储维持当前行为。分布式防重放需要由部署方提供共享 `NonceStore` 实现；生产凭证来源可通过 `CredentialProvider` 扩展。
