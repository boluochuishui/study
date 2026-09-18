# 验证记录

## 自动化回归

执行命令：

```text
mvn test
```

执行结果：

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

其中本地 MySQL 集成测试默认跳过，需要通过系统参数显式启用，避免普通构建依赖开发者本机数据库。

## 本地 MySQL 集成验证

执行命令：

```text
mvn test -Dtest=ConfigDataServiceIntegrationTests -Ddatabase.integration.enabled=true
```

执行结果：

```text
MySQL: 8.0
Flyway migrations validated: 2
Schema version: V2
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

已验证：

- 使用本地配置连接 `localhost:3306/content_risk`。
- 首次启动自动建立本地数据库。
- Flyway 创建历史表并执行 V1、V2。
- 配置项新增成功。
- 按命名空间和配置键查询成功。
- 命名空间列表查询成功。
- 配置项禁用后不再被启用查询返回。
