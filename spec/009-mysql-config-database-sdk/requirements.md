# 需求说明

## 背景

项目业务检测结果不写入数据库，MySQL 主要保存管理台配置、鉴权规则、租户信息和规则阈值等配置数据。数据库操作需要集中封装，业务模块只依赖数据服务接口，后续可以从项目中拆分为独立 SDK。

## 目标

- 接入本地 MySQL `localhost:3306`，默认数据库名为 `content_risk`。
- 使用 MyBatis-Plus 提供基础 CRUD、分页和乐观锁能力。
- 数据库 SDK 内部包含 model、service、entity、mapper 和 MyBatis 实现。
- 业务调用方只依赖 `api.model` 和 `api.service`，不直接依赖 Mapper。
- 建库、建表和结构变更 SQL 使用独立文件保存并按版本迭代。
- 提供通用配置项数据模型，验证 SDK 完整读写链路。
- 数据库密码仅保存在被 Git 忽略的本地配置或环境变量中。

## 非目标

- 检测任务和检测结果不写入 MySQL。
- 本需求不实现管理台 Controller。
- 不实现动态数据源、读写分离和分库分表。
- 不把 MyBatis-Plus Wrapper、Mapper 或 Entity 暴露给业务模块。
- 不允许修改已经在环境中执行过的 Flyway 历史迁移文件。

## 验收标准

- Spring Boot 可以使用 `local` Profile 连接本地 MySQL。
- Flyway 可以建立版本历史并依次执行 V1、V2 迁移。
- `ConfigDataService` 可以新增、查询、列出和禁用配置项。
- 自定义查询 SQL 保存在 Mapper XML 文件中。
- 完整测试和真实 MySQL 集成测试通过。
