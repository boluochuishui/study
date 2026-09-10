# 设计说明

## 分层设计

日志模块分为三层：

```text
业务/框架模块
    -> BizLogRecorder 或 LogRecorder
    -> BaseLogEvent
    -> CompositeLogRecorder
    -> Slf4jLogRecorder / 后续扩展 LogSink
```

## 核心模型

`BaseLogEvent` 是所有日志类型共用的结构化事件模型，包含：

```text
logSpace    日志空间，例如 content-risk、audit、billing
logType     日志类型，例如 API、DEBUG、THIRD_PARTY、CHAIN、BIZ
logLevel    日志级别，例如 TRACE、DEBUG、INFO、WARN、ERROR
traceId     链路追踪 ID
taskId      业务任务 ID
appId       调用方 ID
sceneCode   业务场景
source      日志来源，例如 text.detect.chain 或 third-party.ocr
eventType   事件类型，例如 CHAIN_START、API_ACCESS
message     日志描述
success     是否成功
costMillis  耗时
attributes  扩展属性
occurredAt  发生时间
```

## 扩展方式

后续扩展接口日志、debug 日志、三方日志时，只需要构造不同 `logType` 的 `BaseLogEvent`：

```text
接口日志    logType=API
debug日志   logType=DEBUG
三方日志    logType=THIRD_PARTY
责任链日志  logType=CHAIN
业务日志    logType=BIZ
```

日志空间通过 `logSpace` 区分，例如：

```text
content-risk
audit
billing
system
third-party
```

日志落地方式通过 `LogSink` 扩展，例如：

```text
ApiLogSink          只处理 logType=API
ThirdPartyLogSink   只处理 logType=THIRD_PARTY
ChainLogSink        只处理 logType=CHAIN
AuditLogSink        只处理 logSpace=audit
```

## 责任链日志

责任链上下文 `ChainLogContext` 增加 `logSpace` 字段。责任链监听器在链路开始、节点开始、节点结束、异常、链路结束时生成 `BaseLogEvent`，并设置：

```text
logType=CHAIN
source=chainName
eventType=CHAIN_START / EXECUTE_START / EXECUTE_END / HANDLER_EXCEPTION / CHAIN_END
```

## 兼容业务日志

保留 `BizLogRecorder` 和 `BizLogEvent`，但底层委托给通用 `LogRecorder`。这样业务代码可以继续使用业务日志门面，底层输出能力可以统一扩展。
