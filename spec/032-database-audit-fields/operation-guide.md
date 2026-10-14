# 操作说明

1. 连接 `localhost:3306/content_risk`，确认 V1 到 V7 的业务表已存在。
2. 备份需要保留的数据后执行 `config-data/src/main/resources/db/migration/V8__unify_business_audit_fields.sql`。这是一次性迁移，不可重复执行，MySQL DDL 不支持整份脚本事务回滚。中途失败时先检查已增加的列，再从尚未执行的语句继续。
3. 查询 `information_schema.columns`，确认 12 张业务表各有四个审计字段且均非空。
4. 部署新管理台/数据库 SDK。用户操作应保存当前认证用户 ID，系统初始化和没有用户上下文的写入保存 `0`。
5. 本机 JDBC 集成测试通过 `-Ddatabase.audit.integration.enabled=true` 显式启用，并使用 `local` 数据源配置。测试写入放在事务中回滚，不创建永久测试用户或修改既有业务配置。

后续新建业务表沿用同一四字段约定。`flyway_schema_history` 由第三方维护，不作为业务表进行字段统一。
