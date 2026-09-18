# 设计说明

## 链路路由

检测服务根据 `contentType` 选择固定链：

```text
TEXT  -> text.detect.chain
IMAGE -> image.detect.chain
AUDIO -> audio.detect.chain
VIDEO -> video.detect.chain
```

## 初始化与执行时序

```mermaid
sequenceDiagram
    autonumber
    participant Spring as Spring 容器
    participant Loader as ChainPropertiesResourceLoader
    participant DefLoader as ChainDefinitionLoader
    participant Registry as ChainHandlerRegistry
    participant Validator as ChainDefinitionValidator
    participant Repo as ChainDefinitionRepository
    participant Auth as AuthenticationFilter
    participant Api as DetectController
    participant Service as DetectService
    participant Runtime as ChainRuntime
    participant Executor as ChainExecutor
    participant Listener as ChainExecutionListener
    participant Handler as ChainHandler Bean
    participant ExHandler as ChainExceptionHandler

    rect rgb(244, 248, 255)
        Note over Spring,Repo: 应用启动阶段：责任链初始化
        Spring->>Registry: 注入所有 ChainHandler Bean
        Registry->>Registry: 按 BeanName 建立处理器索引
        Spring->>Loader: 扫描 classpath:/chains/*.chain.properties
        Loader-->>DefLoader: 返回链路配置资源
        DefLoader->>DefLoader: 解析 chain.name、execute.handler、finally.handler、exception.handler
        DefLoader->>Registry: 按 bean&name 解析节点 Bean
        Registry-->>DefLoader: 返回节点处理器实例
        DefLoader-->>Validator: 构建 ChainDefinition
        Validator->>Validator: 校验链名称、节点顺序、超时、异常处理器
        Validator-->>Repo: 保存可执行链定义
        Repo-->>Spring: 初始化完成，责任链不可热加载
    end

    rect rgb(250, 247, 240)
        Note over Auth,ExHandler: 请求执行阶段：系统鉴权后按内容类型选择链并执行
        Auth->>Auth: 匹配 URL 规则并校验 HMAC 签名
        Auth->>Api: 写入 AuthenticationContext 后放行
        Api->>Service: 提交 DetectRequest
        Service->>Service: 构建 DetectContext，写入 taskId、traceId、contentType、日志上下文
        Service->>Runtime: execute(chainName, DetectContext)
        Runtime->>Repo: 获取固定 ChainDefinition
        Repo-->>Runtime: 返回链路定义
        Runtime->>Executor: 执行链路
        Executor->>Listener: onChainStart(context)

        loop execute.handler 节点顺序执行
            Executor->>Listener: onNodeStart(node, context)
            Executor->>Handler: handle(context)
            alt 节点执行成功
                Handler-->>Executor: ChainNodeResult
                Executor->>Listener: onNodeSuccess(node, result, context)
            else 节点抛出异常
                Handler--xExecutor: Exception
                Executor->>Listener: onNodeError(node, exception, context)
                Executor->>ExHandler: handle(exception, context)
                ExHandler-->>Executor: 异常处理结果
            end
        end

        opt finally.handler 已配置
            Executor->>Handler: handle(context)
            Handler-->>Executor: ChainNodeResult
        end

        Executor->>Listener: onChainFinish(result, context)
        Executor-->>Runtime: ChainExecuteResult
        Runtime-->>Service: 检测链执行结果
        Service-->>Api: DetectResult
    end
```

## 文本链

```text
参数校验 -> 关键词检测 -> 正则检测 -> 模拟文本模型 -> finally
```

## 图片链

```text
参数校验 -> 图片下载 -> OCR -> 关键词检测 -> 模拟图片模型 -> finally
```

## 音频链

```text
参数校验 -> 音频拉取 -> ASR -> 关键词检测 -> 模拟文本模型 -> finally
```

## 视频链

```text
参数校验 -> 视频拉取 -> 抽帧/OCR -> 关键词检测 -> 模拟文本模型 -> finally
```

## 说明

图片、音频、视频节点会把 OCR、ASR、抽帧 OCR 产生的文本写入 `DetectContext.derivedTexts`，后续复用文本关键词和文本模型节点。
