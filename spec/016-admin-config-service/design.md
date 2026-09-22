# 016 设计

## 模块边界

`admin-service` 只依赖 `config-data`，使用独立 Spring Boot 入口和默认 8081 端口。管理 API 调用 `ConfigDataService`，不直接使用 Mapper。数据 SDK 新增包含停用项的查询和 `saveIfVersion`；`@Version` 乐观锁与数据库唯一键共同防止并发覆盖。现有检测服务不受新管理台令牌过滤器影响。

## API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/configs?namespace=...` | 列出命名空间配置，当前最多 500 项 |
| GET | `/api/admin/configs/{namespace}/{key}` | 查询配置，包含已停用项 |
| PUT | `/api/admin/configs/{namespace}/{key}` | 创建或覆盖配置 |
| POST | `/api/admin/configs/{namespace}/{key}/disable` | 带版本条件停用配置 |

PUT 请求体包含 `value`、`enabled`、`description`、`version`。新建时 `version=0`，更新时使用 GET 返回的版本。停用请求体只包含 `version`。成功返回 `SUCCESS` 和当前数据；冲突返回 `CONFIG_CONFLICT`/409，不存在返回 `CONFIG_NOT_FOUND`/404。保留配置值为通用字符串，不在管理台解析或执行 SpEL。

## 认证与国际化

启动时要求 `ADMIN_TOKEN` 非空。所有 HTTP 请求使用 `Authorization: Bearer <token>`，令牌按恒定时间字节比较。单令牌仅用于第一版个人开发和受信内网部署；生产环境应走 TLS、网关/身份平台、权限控制与密钥轮换，不应把令牌写入 Git 或命令行日志。

响应消息使用 `messages_zh_CN.properties` 与 `messages_en.properties`，按照 `Accept-Language` 选择；默认简体中文。Java 异常描述为英文，响应层用错误码映射本地化文案。审计日志记录命名空间、键、版本和状态，不记录配置值。

## 数据与部署

管理台连接现有 `sdk_config_item` 表，复用 `config-data` Jar 内 Mapper XML；数据库建表由既有 Flyway 迁移或预先初始化负责。管理台独立打包 Jar 和 Docker 镜像，手动发布流水线上传两个服务的 Jar，构建两个镜像。

当前不包含浏览器页面、细粒度 RBAC、持久化审计表或规则语义校验。这些应在多管理员、生产发布前补齐。

