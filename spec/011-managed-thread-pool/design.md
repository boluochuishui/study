# 011 通用线程池工厂设计

## 配置模型

配置前缀为 `base-sdk.thread-pool`。`defaults` 定义公共默认值，`pools` 按名称覆盖指定参数。

## 核心组件

- `ThreadPoolProperties`：绑定启动配置。
- `ThreadPoolDefinition`：保存合并并校验后的不可变配置。
- `ManagedThreadPoolFactory`：创建、缓存和关闭具名线程池。
- `ContextAwareThreadPoolExecutor`：在任务提交时复制 MDC。
- `LoggingAbortPolicy`：记录拒绝时的线程池状态并抛出异常。

工厂返回标准 `ThreadPoolExecutor`，业务模块可将结果注册为具名 Spring Bean，供 `CompletableFuture` 显式使用。

## 生命周期

工厂由 Spring 管理。应用关闭时先调用 `shutdown`，等待配置的秒数；超时后调用 `shutdownNow`。
