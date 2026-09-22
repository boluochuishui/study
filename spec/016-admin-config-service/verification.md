# 016 验证

## 已验证

- 根目录 `mvn --batch-mode --no-transfer-progress clean verify -q` 通过，包含 Checkstyle、SpotBugs、JaCoCo 门禁。
- 共 51 个测试通过：`base-sdk` 16、`config-data` 4、`detect-service` 23、`admin-service` 8。
- 管理台测试覆盖未授权、中英文响应、默认中文、版本冲突、停用、无效输入和缺失令牌配置；管理台行覆盖率约 89.01%。
- 管理台生成 `admin-service/target/study-admin-service-0.0.1-SNAPSHOT.jar`；Jar 包含 `config-data` 和语言资源，不包含检测业务及 RocketMQ 客户端。
- 使用临时测试令牌启动该 Jar 成功，Tomcat 正常监听随机端口；真实 HTTP 请求不带令牌且指定 `Accept-Language: en` 时返回 401、`UNAUTHORIZED` 和英文消息。检查后已停止测试进程。
- `git diff --check` 无空白错误，只有 Windows 行尾提示。

## 启动与调用

先按 `config-data/src/main/resources/db/migration` 完成数据库迁移，设置 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DATABASE`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` 和 `ADMIN_TOKEN`，再从仓库根目录执行：

```text
java -jar admin-service/target/study-admin-service-0.0.1-SNAPSHOT.jar
```

默认端口 8081。请求使用 `Authorization: Bearer <ADMIN_TOKEN>`，可通过 `Accept-Language: en` 获取英文消息。PUT 新建配置时 `version=0`，更新时使用查询返回的版本。

## 尚未验证

- 本机 Docker Desktop Linux 引擎未启动，无法实测镜像构建。
- 未接入实时 MySQL 进行端到端持久化测试；数据层并发测试使用模拟 Mapper，SQL 路径和 MyBatis 插件仍需在实际数据库中验证。
