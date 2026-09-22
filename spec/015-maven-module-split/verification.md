# 015 验证

## 构建与测试

- 根目录执行 `mvn --batch-mode --no-transfer-progress clean verify -q`，退出码 0。
- `base-sdk` 16 个测试、`detect-service` 23 个测试，均无失败或错误。
- Checkstyle、SpotBugs 及检测服务 JaCoCo 60% 行覆盖率门禁通过；检测服务实际行覆盖率约 73.48%（532/724）。
- 生成 `base-sdk/target/base-sdk-0.0.1-SNAPSHOT.jar`、`config-data/target/config-data-0.0.1-SNAPSHOT.jar` 和 `detect-service/target/study-detect-service-0.0.1-SNAPSHOT.jar`。
- 检查 Jar 内容：`config-data` 包含 3 个 Flyway 迁移文件及 Mapper XML；检测服务可执行 Jar 包含两个依赖模块的 Jar。
- `git diff --check` 无空白错误；Git 仅提示 Windows 行尾转换。

## 未在本机验证

- Docker CLI 可用，但 Docker Desktop Linux 引擎未启动，无法执行镜像构建。
- GitHub Actions 发布流程需要在远程手动触发后验证。
