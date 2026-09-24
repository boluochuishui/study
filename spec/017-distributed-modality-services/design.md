# 017 设计

## 模块

| 模块 | 职责 |
| --- | --- |
| `detect-contract` | RPC 接口、HTTP/RPC DTO、任务及结果事件 |
| `access-service` | HTTP、鉴权、校验、taskId/traceId、RPC 路由、降级 |
| `detect-worker-core` | 责任链执行适配、Kafka 任务与结果基础设施、共享节点 |
| `text-detect-service` | 文本责任链，提供同步和异步 RPC |
| `image-detect-service` | 图片责任链，提供同步和异步 RPC |
| `audio-detect-service` | 音频责任链，只提供异步 RPC |
| `video-detect-service` | 视频责任链，只提供异步 RPC |

入口只依赖契约、基础 SDK 和鉴权配置数据，不依赖 Worker Core。四个检测服务依赖 Worker Core，各自只携带自己的执行器、专属节点和一个链配置。共享的参数校验、文本规则和最终清理节点由 Worker Core 提供。

## 调用链

同步文本/图片：

```text
HTTP -> AuthenticationFilter -> access-service -> Dubbo Triple -> modality chain -> HTTP result
```

异步音视文图：

```text
HTTP -> access-service -> Dubbo submit -> modality Kafka task topic -> modality chain
     -> detect-result topic
```

`taskId` 和 `traceId` 均由入口生成并在 RPC、Kafka、责任链和日志中保持不变。Kafka Producer 使用 `taskId` 作为 Key。Topic 默认值为 `detect-task-text`、`detect-task-image`、`detect-task-audio`、`detect-task-video`；Consumer Group 与模态对应，结果 Topic 默认为 `detect-result`。

## 可用性语义

同步 RPC 关闭自动重试，异常时返回 `FAILED + PASS + degraded=true`，错误码为 `DETECT_SERVICE_UNAVAILABLE`。异步只有 Kafka Broker 确认写入后才返回 HTTP 202、`ACCEPTED` 和 `resultExpected=true`；RPC 或 Kafka 失败返回 HTTP 200、`FAILED + PASS + degraded=true + resultExpected=false`。本地默认 `logging` 模式不伪造入队成功，异步请求明确降级。

Kafka 消费是至少一次语义，第一版不持久化任务状态，也未加入 Redis 幂等锁。结果消费者应按 `taskId` 去重；后续可在 Worker 增加带 TTL 的幂等记录。结果发送失败只记录完整错误，符合当前允许少量放通和结果丢失的业务取舍。

## RPC 与服务发现

使用 Dubbo 3.3.6 Triple 协议。RPC record 明确实现 `Serializable`，满足 Dubbo 严格序列化检查。开发环境默认通过 `TEXT_DETECT_RPC_URL` 等变量直连本机 50051-50054；注册中心通过 `NACOS_ADDRESS` 配置，生产环境应改为 Nacos 服务发现及治理配置，不应依赖本地默认地址。

## 第一版边界

- 已实现 Kafka 发布、消费和结果事件代码，但没有在本机启动 Kafka 做真实 Broker 联调。
- 已验证 HTTP 到文本 Triple RPC 的跨进程调用；图片、音频、视频通过相同契约和独立上下文测试验证。
- 未加入 Redis 幂等、死信重放、结果回调服务、Nacos/Kafka 部署清单和 Kubernetes HPA/KEDA。
- 配置热加载框架保持通用能力，本需求暂不把具体规则配置接入四个 Worker。
