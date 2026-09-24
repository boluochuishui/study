# 019 验证

## 自动化验证

- 管理台发布合法规则后生成新版本和不可变 JSON 快照。
- 非法或危险表达式无法发布。
- 回滚历史版本时生成新的发布记录。
- 热加载数据源能够生成全量结果及有序增量变更。
- 热加载模块能够将表达式源码编译为内存规则目录。
- Spring Boot 默认关闭规则数据库热加载，开启后按场景加载。

## 验证结果

- `mvn --batch-mode --no-transfer-progress clean verify -q` 执行成功。
- 共生成 24 份测试报告，执行 73 个测试：0 失败、0 错误、2 跳过。
- Checkstyle、SpotBugs 和 JaCoCo 校验通过，所有服务 Jar 构建成功。
- Flyway V4 已在本地 MySQL `content_risk` 数据库执行成功，规则集、草稿和发布表创建完成。
- 默认配置不会连接规则数据库；开启 `detect.rules.spel.enabled` 后，检测服务按自身场景执行全量和增量加载。
- 使用本地 MySQL 实际开启文本服务热加载后启动成功，空数据全量快照激活结果为 `cursor=0, itemCount=0`。
