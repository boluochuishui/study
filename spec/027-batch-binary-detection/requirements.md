# 027 检测输入与异步多样本能力

## 当前确认范围

- 一个请求只包含一种 contentType，由服务端生成一个 taskId。
- 原同步接口 /api/detect/sync 只支持 TEXT、IMAGE，items 必须恰好一个；不提供同步批量能力。
- 原异步接口 /api/detect/async 支持 TEXT、IMAGE 多样本；AUDIO、VIDEO 第一版仍限制一个样本。
- 不新增按数量区分的接口，不保留 BatchController、BatchService、BatchCommand 等平行类和 detectBatch/submitBatch RPC 方法。
- 在原 DetectRequest、DetectCommand、DetectResult、DetectService、DetectRequestValidator 及 Worker 中改造；单样本与多样本共用 items/results。
- 文本使用 text，媒体可使用统一 url、标准 Base64 data。
- 非空原数据优先于 URL；无效原数据明确失败，不回退 URL。
- itemId 请求内唯一；language、region、sceneCode 请求共用；appId 来自鉴权请求头，不接受请求体覆盖。
- 数量、文本长度、文件大小、总样本体积、图片像素数限制从 resources 配置加载，有默认值和环境变量覆盖；同步数量固定为一。
- 协议、重复编号、请求总量错误返回 400；Base64 格式/解码后大小错误保留为逐项失败结果，其余继续。
- 每项独立责任链上下文，记录 taskId/itemId 日志，不对外暴露步骤、原数据或存储引用。
- 异步仅发送一条任务消息和一条汇总结果消息，不逐项发 MQ；投递重复仍由消费方按 appId+taskId 幂等。
- 不写任务数据库；沿用明确降级放通、详细异常日志及现有真实检测能力边界。

## 验收要点

同步单样本成功、同步多样本拒绝且无 RPC/文件落盘；异步文本/图片多样本接受、音视频多样本拒绝；JSON 原始字节验签；Base64 原数据优先；无效样本不回退；大小与数量边界；独立逐项上下文；一个任务编号和一条汇总消息；旧 Batch 类与 RPC 方法清除。

## 修订记录

2026-10-13：根据用户确认，撤销早期同步批量方案，移除平行 Batch 模型，补充 Base64 输入优先级。此前验证记录保留在 verification.md 供追溯，以本版需求和设计为准。

## 2026-10-13 JSON 输入与 DTO 修订

用户取消直接上传二进制文件能力，只保留两个 application/json 检测入口。删除 uploadSync/uploadAsync、filePartName、multipart 校验及其签名实现，multipart 检测请求返回 415。媒体样本通过 data（标准 Base64）或 url 传入，Base64 优先级与原有数量限制不变。

HTTP 请求和响应 DTO 使用普通 Java 类、标准 getter/setter、无参构造器和 Fastjson2 @JSONField 字段映射；Fastjson2 依赖版本由父 POM 统一管理。内部 RPC 命令、任务消息和基础设施 record 本次不调整。Spring MVC 仍采用现有 Jackson，字段名称保持一致，不在本需求中整体替换序列化框架。
