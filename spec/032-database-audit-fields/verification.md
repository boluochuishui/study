# 验证记录

## 本地迁移

2026-10-13 已在 `localhost:3306/content_risk` 执行 V8。通过 `information_schema.columns` 验证，12 张业务表各有 `created_by`、`created_at`、`updated_by`、`updated_at`，共 48 个字段且全部 `NOT NULL`。`flyway_schema_history` 未修改。

## 自动验证

```text
mvn -pl admin-service -am verify -Ddatabase.audit.integration.enabled=true
```

执行成功，包含相关模块单元测试、Checkstyle 和 SpotBugs。

新增单元测试覆盖嵌套操作人恢复、异常清理、异步线程默认系统身份，以及更新不覆盖创建字段。

真实 MySQL 集成测试验证 MyBatis-Plus 配置/SpEL 写入和 XML 用户、角色、授权关联、令牌、在线测试记录写入。使用操作人 101 创建、102 更新，确认创建人保持 101、更新人为 102；配置创建时间保持不变。登录令牌与测试记录能够显式使用用户 ID。

所有集成写入在测试事务结束时回滚，查询确认没有残留 `audit.%` 用户或 `audit-test` 配置。自增 ID 因测试插入可能出现正常空隙。

测试同时发现并修复在线测试记录构造参数映射：MyBatis 的 `long`/`int` 别名表示包装类型，原 DTO 使用基本类型，改用 `_long`/`_int` 后实际查询成功。
