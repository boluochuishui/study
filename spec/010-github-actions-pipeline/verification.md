# 验证记录

## Maven 验证

执行：

```text
mvn --batch-mode --no-transfer-progress clean verify
```

结果：

- 构建成功。
- 单元测试共 21 个，失败 0 个，错误 0 个，按配置跳过 2 个数据库集成测试。
- Checkstyle 违规 0 个。
- SpotBugs 问题 0 个。
- JaCoCo 报告成功生成，实际行覆盖率为 69.12%，60% 门禁通过。
- Spring Boot 可执行 Jar 成功生成。

## Docker 验证

Dockerfile 和 `.dockerignore` 已完成。当前机器的 Docker Desktop Linux Engine 未运行，因此本地 `docker build` 无法连接 daemon；镜像构建需要在 GitHub 托管 Runner 首次运行后完成最终验证。

## 生成位置

```text
target/surefire-reports
target/spotbugsXml.xml
target/site/jacoco/index.html
target/study-*.jar
```
