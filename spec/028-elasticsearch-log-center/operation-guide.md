# 接入与使用

1. 创建 ES data stream 索引模板：`PUT /_index_template/contentrisk-logs`，请求体使用 `deploy/logging/contentrisk-template.json`。生产环境应设置符合本地保留要求的数据流生命周期或 ILM，并设置副本、访问权限。
2. 用 Elastic Agent/Fleet 的 Kubernetes 日志集成采集本项目容器标准输出。只将合法的单行 JSON 且含 `eventId`、`tenantId` 的事件导入 `logs-contentrisk-app-dev`，解析 JSON 到文档根字段；普通 SLF4J 文本行留在普通应用日志数据集。采集器需要访问节点容器日志，并监控解析失败和采集积压。
3. 单租户本地部署设 `LOG_STRUCTURED_ENABLED=true`、`LOG_TENANT_ID=1`。当前 `deploy/k8s/local/configmap.yaml` 已配置。正式部署必须按租户实例设置正确编号。管理台进程另设 `LOG_ES_URL`、`LOG_ES_API_KEY`、`LOG_ES_DATA_STREAM=logs-contentrisk-*`。API Key 至少需要目标 data stream 的读权限，通过 Secret 注入。
4. 执行 `config-data/src/main/resources/db/migration/V6__log_read_permission.sql`；已有 `SUPER_ADMIN` 自动得到 `LOG_READ`，其他角色可在管理台授权。管理台登录后访问 `/logs`。
5. 在 Kibana 验证 `taskId`、`tenantId`、`eventId`、`@timestamp` 是否可检索，再在管理台按任务查询。数据流没有文档时页面显示空列表；ES 未配置或不可用返回 503。

本地 Kubernetes 清单尚未包含管理台、ES 或采集器部署。需要先独立启动这些组件并完成采集配置。当前直接打印的 JSON 与 SLF4J 文本共享 stdout，采集器应按 JSON 事件筛选后写入专用 data stream。
