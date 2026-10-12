# 设计

```text
检测服务 BaseLogEvent -> JsonConsoleLogSink -> JSON stdout
                                      -> 采集器 -> ES data stream
管理台浏览器 -> admin-service LOG_READ + tenantId -> ES -> 表格/详情/任务时间线
```

`JsonConsoleLogSink` 为可选输出端。部署时配置 `LOG_STRUCTURED_ENABLED=true` 和正数 `LOG_TENANT_ID`。只输出显式字段，不序列化动态 `attributes`、请求正文、密钥或二进制。业务日志仍按原有 SLF4J 输出。`CompositeLogRecorder` 隔离输出端异常，不阻断检测。

服务端使用可信 `AdminSecurityContext` 的租户编号构造 ES `term tenantId` 条件。外部不能提交租户条件。索引模板将高频筛选字段定为 `keyword`，关闭动态映射。响应只返回白名单字段。查询区间最大 30 天，列表大小最大 100，任务时间线最大 500。列表按时间逆序和事件 ID 排序，游标使用两个排序值；任务按时间正序。

`LOG_TENANT_ID` 只适用于每个检测实例服务一个租户的部署。当前请求鉴权和 RPC 命令没有可信的租户传递，不应在共用多租户检测实例上启用固定租户输出。多租户共享实例需要先扩展 appId 与租户绑定和 RPC/MQ 的租户字段。

此处的任务时间线是日志视图，不保证完整；异步步骤记录与检测主流程状态独立。管理台不会把缺失日志视为通过或失败。
