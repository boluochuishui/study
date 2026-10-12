# 设计方案

## 责任链

```text
参数校验
  -> 文本归一化及 SHA-256
  -> Redis 违规缓存查询（命中则短路）
  -> 关键词检测
  -> 正则检测
  -> 大模型检测（仅 ASYNC 且已启用）
  -> Redis 违规结果写入
  -> 最终决策输出
  -> finally
```

关键词、正则和大模型节点只向上下文追加动作与标签，不提前中断。缓存写入节点仅在汇总结论为 `REJECT` 时写入，随后最终决策节点输出责任链结果。

## 缓存模型

Redis Key：

```text
{key-prefix}:{decision-version}:{appIdHash}:{sceneHash}:{contentSha256}
```

其中 appId、sceneCode 和归一化文本均以 SHA-256 表示。Redis Value 只包含动作、标签、决策版本和缓存时间，不包含原文、命中词或模型输入。

`decision-version` 用于规则语义变化后的缓存隔离，TTL 用于自动回收历史违规样本。

## 大模型边界

`TextLargeModelClient` 是供应商无关 SPI，默认实现直接返回无风险结果。大模型节点同时满足以下条件才调用 SPI：

- 命令执行模式为 `ASYNC`；
- `detect.text.large-model.enabled=true`。

客户端异常只记录 taskId、模型类型和异常，不记录文本，并继续使用本地检测结论。

## 异常策略

- Redis 查询失败：视为未命中，继续检测。
- Redis 写入失败：保留本次检测结论，仅记录错误日志。
- 大模型失败：保留本地规则结论，仅记录错误日志。
- 其他责任链异常：沿用项目现有异常策略和放通降级语义。
