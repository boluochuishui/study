# 设计说明

## 流水线划分

```text
ci.yml
  push / pull_request
  -> JDK 21
  -> mvn clean verify
  -> 上传质量报告

release.yml
  workflow_dispatch(version)
  -> 校验并设置版本
  -> mvn clean verify
  -> 上传 Jar 和质量报告
  -> Buildx 构建 Docker 镜像
```

CI 与发布构建不在 YAML 中分别拼装检查命令，而是共同调用 Maven `verify` 生命周期，避免本地、CI 和发布环境出现不同的规则组合。

## Maven 质量门禁

| 工具 | 生命周期阶段 | 失败策略 |
| --- | --- | --- |
| Checkstyle | `validate` | 存在违规立即失败 |
| Surefire | `test` | 测试失败立即失败 |
| Spring Boot | `package` | 生成可执行 Jar |
| SpotBugs | `verify` | Medium 及以上问题失败 |
| JaCoCo | `verify` | 生成报告并检查行覆盖率不低于 60% |

Checkstyle 仅检查 Java 源码。SpotBugs 对 Spring 容器管理对象产生的明确误报使用版本化过滤文件排除，领域集合仍通过防御性复制解决实际可变性风险。

## 制品策略

- CI 报告保留 14 天。
- 手动发布构建的 Jar 和质量报告保留 30 天。
- Docker 镜像当前只验证构建，不登录或推送仓库。
- 后续接入 GHCR 时，可增加 `packages: write` 权限和 Docker 登录步骤，不需要修改 Dockerfile。

## Docker 运行时

Dockerfile 只复制已经过质量门禁的 Jar，使用 Java 21 JRE 作为运行时，并通过专用 `application` 用户启动，避免容器内使用 root 运行应用。
