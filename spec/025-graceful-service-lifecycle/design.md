# 设计方案

## 模块

`base-sdk.lifecycle` 提供状态机、工作租约、生命周期参与者、配置及自动装配。`lifecycle.web` 提供健康检查、受保护的摘流接口和 HTTP 准入过滤器。`detect-worker-core.worker.lifecycle` 适配 Kafka 和固定版本 Dubbo 3.3.6。

## 上线

Spring 初始化责任链及其他 Bean；`HotLoadManager` 的 ApplicationReadyEvent 监听器优先加载全量快照。生命周期管理器随后检查所有已注册热加载模块、RPC 导出状态和 Kafka 容器。全部检查成功后发布 READY 并启动 Kafka 消费。检查失败则应用启动失败，保持拒绝流量。RPC 注册可能早于 READY，应用准入门控制该窗口内的调用。

## 下线

preStop 请求本机摘流接口，立即将状态切换为 DRAINING、暂停 Kafka、注销 Dubbo 接口级及应用级注册。等待路由传播后发送 SIGTERM。ContextClosedEvent 中优先执行协调器，等待所有工作租约释放，停止消费者，再关闭线程池。之后 Spring 和 Dubbo 正常销毁剩余资源。

HTTP `/api/*` 租约覆盖过滤器和 Controller。RPC 的同步检测、任务入队分别持有租约。Kafka 处理器持有覆盖检测和结果发送的单一租约，并调用 `detectAccepted` 避免下线中途重复准入。结果队列等待发送确认，超时、异常记录后结束租约。下线期间尚未接收的 Kafka 记录使用无限退避直到容器停止，避免按普通异常跳过记录。

## 时间预算

默认应用收尾 40 秒、Spring 每阶段 45 秒、Pod 总宽限 120 秒，preStop 网络调用最多 5 秒，额外传播等待 5 秒。应用协调器的在途等待、Kafka 停止和线程池收尾共用截止时间。框架后续关闭阶段仍可能消耗时间，Pod 宽限是最后上限。参与者必须有限等待；第三方注册中心操作受其自身 RPC 超时约束。

## 安全与限制

摘流要求实际对端为 loopback，并校验 X-Drain-Token，不信任转发头。空令牌禁用手工摘流，SIGTERM 仍可关闭。部署脚本生成随机令牌并存入 Secret。liveness 不绑定 Redis/MySQL 瞬时健康，避免依赖故障引发重启风暴。该实现针对现有同步 Servlet Controller；未来 Servlet 异步请求需延长租约到 AsyncListener 完成。

无注册中心时依靠 Kubernetes Endpoint 更新和准入门。Dubbo 适配器调用内置 Offline/OfflineApp 逻辑，不启动公网 QoS。单 Pod 被直接删除时不承诺可用性；滚动更新需要资源足够创建临时副本。超时不等于业务线程已强制终止，最终由进程退出结束；Kafka 未提交任务可能被重投，结果消费方仍需幂等。
