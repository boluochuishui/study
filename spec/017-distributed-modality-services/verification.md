# 017 验证

## 自动验证

- 根目录执行 `mvn --batch-mode --no-transfer-progress clean verify -q`，55 个测试、Checkstyle、SpotBugs 和既有 JaCoCo 门禁通过；接入服务行覆盖率约 83.47%。
- 四个模态服务分别启动 Spring Boot/Dubbo 测试上下文，均只加载自身链配置并成功发布 RPC 服务。
- Worker Core 覆盖同步结果映射、异步入队成功和未入队降级；入口测试覆盖四模态 RPC 路由、同步模式限制和 RPC 故障开放。
- 异步接口仅在入队确认后返回 HTTP 202；未入队时返回 HTTP 200、`PASS`、`degraded=true` 和 `resultExpected=false`。

## 跨进程验证

分别启动 `text-detect-service` 和 `access-service` Jar，向 `POST /api/detect/sync` 发送合法 HMAC-SHA256 文本请求。响应为 HTTP 200，包含服务端生成的 `dt_` 任务号、`SUCCEEDED`、`PASS` 和 `degraded=false`；文本服务日志记录了对应 Triple 调用和责任链执行。测试进程已停止。

首次联调时 Dubbo 严格检查发现 RPC record 未实现 `Serializable`，修复契约后重新联调通过。

## 尚未验证

- 本机未启动 Kafka 和 Nacos，尚未执行真实异步消息、Consumer Group 再均衡及注册中心发现测试。
- Docker Desktop Linux 引擎未启动，六个服务镜像由手动发布流水线继续验证。
