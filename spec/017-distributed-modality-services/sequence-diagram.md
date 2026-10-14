# 接入、检测与降级放通时序图

本文根据当前代码绘制，涵盖服务启动、同步检测、异步受理及结果消息。`PASS` 表示处置建议；降级时仍使用 `FAILED` 和 `degraded=true` 表明检测没有成功完成。

## 1. 接入服务独立启动

```mermaid
sequenceDiagram
    autonumber
    participant Boot as Spring Boot
    participant Access as 接入服务
    participant Client as 四类 Dubbo 客户端
    participant Worker as 下游检测服务
    participant Life as 生命周期管理

    Boot->>Access: 启动应用并加载配置
    Access->>Client: 注入文本、图片、音频、视频 RPC 引用
    Note over Client: check=false、init=false、lazy=true<br/>timeout=3000ms、retries=0
    Note over Client,Worker: 启动阶段不要求下游已启动<br/>首次业务调用时才建立和使用 RPC 引用
    Access->>Life: 完成启动并进入 READY
    Note over Access,Life: 接入 READY 表示入口可接收请求<br/>不保证四类检测服务可用
    Note over Worker: 可以稍后启动所需的检测服务
```

上述图只说明下游 RPC 可用性不会阻止接入启动；接入自身配置、端口及其他依赖仍需满足启动条件。

## 2. 同步检测及 RPC 降级

同步接口仅支持文本、图片单样本。鉴权与请求级校验失败返回错误，不进入 RPC 降级放通分支。

```mermaid
sequenceDiagram
    autonumber
    actor Caller as 调用方
    participant Auth as 鉴权过滤器
    participant API as DetectController
    participant Access as DetectService
    participant Validator as DetectRequestValidator
    participant IDs as TaskIdGenerator
    participant RPC as 对应模态 RPC 客户端
    participant Worker as RPC Provider / WorkerService
    participant Chain as 检测责任链
    participant Log as 日志模块

    Caller->>Auth: POST /api/detect/sync<br/>X-App-Id、鉴权信息、JSON 样本
    alt 鉴权通过
        Auth->>API: 转交请求
        API->>Access: syncDetect(appId, request)
        Access->>Validator: prepare(request, SYNC)
        Note over Validator: 校验类型、单样本、语种、地区、大小等<br/>媒体原数据存在时优先使用原数据
        alt 请求级校验失败
            Validator-->>API: 抛出请求异常，经全局异常处理器转换
            API-->>Caller: HTTP 400 + 参数错误信息
        else 请求校验完成
            Validator-->>Access: 准备后的样本列表<br/>样本级准备失败可携带错误信息
            Access->>IDs: 生成 taskId、traceId
            IDs-->>Access: 请求唯一编号及链路编号
            Access->>Access: 构造 SYNC DetectCommand<br/>携带 App、场景、样本及编号
            Access->>RPC: TEXT / IMAGE 对应客户端 detect(command)
            alt 下游可调用且在 RPC 超时前返回
                RPC->>Worker: Dubbo Triple detect(command)
                Worker->>Worker: 校验模态和执行模式，获取生命周期任务租约
                alt 样本准备失败
                    Worker->>Worker: 构造该样本 FAILED + PASS + degraded=true
                else 样本可执行
                    Worker->>Chain: 构造 DetectContext 并执行既定链
                    Chain->>Log: 记录节点与链路过程
                    alt 责任链执行成功
                        Chain-->>Worker: 检测结论与证据
                        Worker->>Worker: 转换样本结果<br/>PASS / REVIEW / REJECT
                    else 责任链失败或执行异常
                        Chain-->>Worker: 失败结果或异常
                        Worker->>Log: 记录执行异常或链失败信息
                        Worker->>Worker: 构造 FAILED + PASS + degraded=true<br/>DETECT_CHAIN_FAILED / DETECT_EXECUTION_FAILED
                    end
                end
                Worker-->>RPC: DetectResult，请求汇总 status=COMPLETED
                RPC-->>Access: 返回检测结果
            else 下游未启动、连接失败、RPC 超时等
                RPC-->>Access: 抛出 RuntimeException
                Access->>Log: ERROR API 事件 SYNC_RPC_DEGRADED<br/>taskId、traceId、App、场景、模态、异常类型
                Access->>Access: DetectResult.degraded(command)<br/>每个样本 FAILED + PASS + degraded=true<br/>DETECT_SERVICE_UNAVAILABLE
            end
            Access-->>API: DetectResult
            API-->>Caller: HTTP 200 + taskId + results<br/>汇总 status=COMPLETED，逐样本读取真实状态
        end
    else 鉴权失败
        Auth-->>Caller: 鉴权错误响应
    end
```

`COMPLETED` 仅表示请求处理结束。调用方需读取 `results` 内的 `status`、`action`、`degraded` 和错误码，不能仅凭 HTTP 200 或汇总状态判断检测成功。入口当前记录 RPC 异常类型；Worker 的执行异常日志包含异常堆栈。

## 3. 异步受理、执行与结果消息

异步接口支持文本、图片、音频、视频；文本和图片可在一个请求中包含多样本。只有任务队列确认入队，服务才返回 `resultExpected=true`。

```mermaid
sequenceDiagram
    autonumber
    actor Caller as 调用方
    participant API as 鉴权 / DetectController
    participant Access as DetectService
    participant Prepare as 请求校验与编号生成
    participant RPC as 对应模态 RPC 客户端
    participant Worker as RPC Provider / WorkerService
    participant TaskMQ as 模态任务队列
    participant Consumer as Kafka Listener / TaskProcessor
    participant Chain as 检测责任链
    participant ResultMQ as 结果队列 detect-result
    participant Receiver as 业务结果消费者
    participant Log as 日志模块

    Caller->>API: POST /api/detect/async<br/>单模态样本列表与鉴权信息
    API->>Access: 鉴权通过后 asyncDetect(appId, request)
    Access->>Prepare: prepare(request, ASYNC)，生成 taskId、traceId
    Note over API,Prepare: 鉴权或请求级校验失败直接返回错误<br/>不进入后续受理步骤
    Prepare-->>Access: ASYNC DetectCommand
    Access->>RPC: 按 contentType 选择 submit(command)
    alt RPC 调用失败
        RPC-->>Access: 连接失败、超时或其他 RuntimeException
        Access->>Log: ERROR API 事件 ASYNC_RPC_DEGRADED
        Access-->>API: FAILED + PASS + degraded=true<br/>resultExpected=false<br/>DETECT_SERVICE_UNAVAILABLE
        API-->>Caller: HTTP 200 + taskId + 降级放通信息
    else RPC 正常返回
        RPC->>Worker: Dubbo Triple submit(command)
        Worker->>Worker: 校验模态和模式，获取生命周期任务租约<br/>构造 DetectTaskMessage
        Worker->>TaskMQ: publish(task)<br/>Kafka Key=taskId，Topic 按模态区分
        alt 队列确认入队
            TaskMQ-->>Worker: publish=true
            Worker-->>RPC: ACCEPTED、resultExpected=true、degraded=false
            RPC-->>Access: 受理响应
            Access-->>API: AsyncDetectAcceptedResult
            API-->>Caller: HTTP 202 + taskId<br/>此时不包含最终检测结论
            Note over Access,Consumer: 后续检测由 Worker 消费任务执行<br/>接入服务不等待检测完成
            TaskMQ->>Consumer: 投递 DetectTaskMessage
            Consumer->>Consumer: 获取生命周期任务租约<br/>按请求顺序处理各样本
            loop 每个样本，使用独立 DetectContext
                Consumer->>Chain: 执行对应模态的检测链
                Chain->>Log: 记录责任链和节点过程
                Chain-->>Consumer: 成功结论或失败信息
                Consumer->>Consumer: 失败样本转 FAILED + PASS + degraded=true<br/>继续处理剩余样本
            end
            Consumer->>Consumer: 汇总为一条 DetectResultMessage<br/>保留逐样本 results、taskId 和总体处置
            Consumer->>ResultMQ: 发布结果，Key=taskId
            alt 结果发送成功
                ResultMQ-->>Receiver: 一请求一条结果消息，包含多个样本结果
                Receiver->>Receiver: 按 taskId 去重并应用处置策略
            else 结果发送失败
                ResultMQ-->>Consumer: 抛出发送异常
                Consumer->>Log: 记录 taskId 和异常堆栈
                Note over Caller,Receiver: 当前不保证结果补发<br/>业务方需根据自身等待期限执行保底策略
            end
        else 入队失败或返回 false
            TaskMQ-->>Worker: 异常或 publish=false
            Worker->>Log: 发送异常时记录 taskId 和异常堆栈
            Worker-->>RPC: FAILED + PASS + degraded=true<br/>resultExpected=false、TASK_QUEUE_UNAVAILABLE
            RPC-->>Access: 降级受理响应
            Access-->>API: 原样返回响应
            API-->>Caller: HTTP 200 + taskId + 降级放通信息
        end
    end
```

本地 `logging` 任务队列不实际入队，会返回 `false`，因此异步接口按 `TASK_QUEUE_UNAVAILABLE` 降级。启用真实 Kafka 才能形成图中的任务消费与结果投递过程。

## 4. 降级返回字段对照

| 场景 | HTTP | 状态 | 处置 | 降级标记 | 后续结果预期 |
| --- | --- | --- | --- | --- | --- |
| 同步 RPC 不可用 | 200 | 请求汇总 COMPLETED，样本 FAILED | 样本 PASS | 样本 true | 不适用 |
| 异步 RPC 不可用 | 200 | FAILED | PASS | true | false |
| 异步任务入队失败 | 200 | FAILED | PASS | true | false |
| 异步任务入队成功 | 202 | ACCEPTED | 受理时为 null | false | true |

异步 RPC 超时具有不确定性：下游可能已经入队，但入口没有收到受理响应。因此 `resultExpected=false` 表示调用方无需等待，不保证绝对没有后续消息。Kafka 消费可能重复投递，结果消费者应按 `taskId` 去重。

同步 RPC 超时也不保证下游检测立即停止。当前 RPC 超时为 3000 ms，Worker 请求执行期限默认 25000 ms；二者属于不同层的等待/执行限制。本文记录当前行为，后续可另行统一超时预算。

## 代码对应

- `access-service`：`DetectController`、`DetectService`、`DetectRequestValidator`、四类 `*DetectRpcClient`。
- `detect-worker-core`：`ModalityWorkerService`、`AsyncDetectTaskProcessor`、`KafkaDetectTaskQueue`、`KafkaDetectResultQueue`。
- `detect-contract`：`DetectResult.degraded`、`DetectItemResult.degradedPass`、`AsyncDetectAcceptedResult`。
