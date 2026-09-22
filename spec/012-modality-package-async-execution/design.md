# 012 音视文图拆包与异步执行设计

## 模块边界

```text
detect
├── common
│   ├── ModalityDetectExecutor
│   ├── AbstractModalityDetectExecutor
│   ├── ModalityDetectRouter
│   └── handler
├── text
├── image
├── audio
└── video
```

各模态执行器绑定固定责任链。责任链节点 Bean 名称保持不变，因此现有 `chain.properties` 无需修改。

## 同步流程

`DetectService -> ModalityDetectRouter -> ModalityDetectExecutor -> ChainRuntime`

## 异步流程

`DetectService -> CompletableFutureDetectTaskDispatcher -> detect-async -> AsyncDetectTaskProcessor -> ModalityDetectRouter -> DetectResultPublisher`

线程池拒绝提交时转换为 `DetectTaskPublishException`；任务执行阶段异常继续遵循故障开放策略，并发布降级结果。结果发布异常由 CompletableFuture 完成回调记录。

## 微服务演进

拆分后枢纽服务可将模态执行器替换为 RPC 客户端。各模态包中的执行器、节点与责任链配置可整体迁移到对应服务。
