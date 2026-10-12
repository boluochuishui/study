# 020 验证

## 自动化验证

- 登录成功、密码错误和令牌撤销。
- 未认证返回 401，权限不足返回 403。
- 用户权限来自当前角色集合。
- 不同租户之间无法读取或更新配置和规则。
- 热加载只读取指定租户的发布记录。

## 验证结果

- `mvn clean test -q`：通过。
- `mvn verify -q`：全工程通过，包含单元测试、Checkstyle、SpotBugs 和 JaCoCo 60% 行覆盖率门禁。
- MySQL 8.0 `content_risk`：Flyway V5 执行成功，`flyway_schema_history` 中版本 5 的 `success=1`。
- 数据校验：初始化 7 个权限、1 个默认租户 `SUPER_ADMIN` 角色，4 张配置及规则业务表均存在 `tenant_id`。
- 首次启动管理台前必须设置 `ADMIN_BOOTSTRAP_PASSWORD`，未配置时拒绝生成弱默认账号。
