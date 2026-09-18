# 验证记录

## 自动化验证

执行命令：

```text
mvn test
```

执行结果：

```text
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

已覆盖：

- 同步文本和图片检测。
- 同步音频请求拒绝。
- 异步文本、图片、音频和视频受理。
- 每次异步提交生成不同的服务端 `taskId`。
- 异步消费者保留原始 `taskId` 并发布终态结果。
- 同步责任链异常返回 `FAILED + PASS + degraded=true`。
- Spring 上下文和现有责任链、日志模块回归测试。

## 待环境验证

- RocketMQ Broker ACK 后接口返回 `202`。
- 消费失败后的重试和死信策略。
- 外部租户 ACL 只能订阅自己的结果 Topic。
- 同一任务重复投递时客户按 `taskId` 幂等处理。
- RocketMQ Proxy、网络中断和服务重启场景。
- Elasticsearch 日志写入、索引生命周期和敏感字段检查。
