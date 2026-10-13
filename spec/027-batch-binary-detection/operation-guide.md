# JSON 检测接口使用说明

## 接口范围

- POST /api/detect/sync：application/json，TEXT/IMAGE，items 恰好一个。
- POST /api/detect/async：application/json，TEXT/IMAGE 默认最多 100 个样本；AUDIO/VIDEO 仍限一个。
- 已取消 multipart 文件上传，相关请求返回 415；不再传 filePartName。
- appId 来自 X-App-Id 鉴权请求头，不使用请求体覆盖。

## 同步文本

```json
{
  "clientRequestId": "business-001",
  "sceneCode": "comment",
  "contentType": "TEXT",
  "language": "zh-CN",
  "region": "CN",
  "items": [{"itemId": "one", "text": "待检测文本"}]
}
```

异步文本或图片可扩展 items，但同一请求只包含一种 contentType。

## 媒体数据

媒体可以传 URL：

```json
{"sceneCode":"image-review","contentType":"IMAGE","items":[{"itemId":"one","url":"https://example.com/image.png"}]}
```

也可以直接传 Base64：

```json
{"sceneCode":"image-review","contentType":"IMAGE","items":[{"itemId":"one","data":"<standard-base64>","url":"https://example.com/backup.png"}]}
```

data 替换为实际文件的标准 Base64，不携带 data:image/... 前缀，不折行。非空 data 优先；原数据无效返回 INVALID_SAMPLE，不回退 URL。空字符串 data 视为未提供，空白字符串是非法样本。

大音视频优先 URL，Base64 的约 4/3 膨胀会计入请求总预算。当前 URL 下载、OCR/ASR/模型等能力仍沿用已有实现，协议校验不等于完整 SSRF 防护或真实模型检测。

## 签名与结果

携带 X-App-Id、X-Timestamp、X-Nonce、X-Signature。沿用原 HMAC-SHA256 规则，对原始 JSON 请求字节（包括 Base64）计算摘要，不重新序列化后签名。不存在 multipart-v1 签名路径。

同步返回 taskId、contentType、status 和 results 数组，只有一个结果。异步入队成功返回 202、resultExpected=true；默认 logging 模式明确返回降级而非假装成功。

异步结果事件 v2.0 使用一条消息携带所有项目结果；降级事件为 CONTENT_DETECT_FAILED。COMPLETED 不代表全部成功。消费者按 appId+taskId 幂等，MQ 重投递可能重复；服务宕机或发布失败允许无结果，遵循既有放通机制。

## 配置与存储

输入预算位于 access-service/src/main/resources/detection-input.yaml，由 application.yaml 引用，支持环境变量覆盖：

| 配置 | 默认 |
| --- | --- |
| study.detect.max-async-items | 100，可向下调整；音视频限 1 |
| study.detect.max-text-length | 每项 5000 个 Unicode 码点 |
| study.detect.max-total-text-bytes | 256 KiB |
| study.detect.max-image-bytes | 10 MiB |
| study.detect.max-media-bytes | 50 MiB |
| study.detect.max-request-bytes | 文本与 Base64 编码累计 64 MiB |
| study.detect.max-image-pixels | 2000 万像素 |
| study.auth.max-body-bytes | 64 MiB 原始 JSON |
| study.worker.request-timeout-ms | 25000，软截止时间 |
| base-sdk.detection-files.retention | 24h |

Base64 解码后仍使用 DetectionFileStore 内部共享文件引用，不将大文件通过 RPC/MQ 传递。所有服务配置相同 DETECT_FILE_ROOT；多节点需 RWX 或对象存储适配。TTL 不等于容量配额，需要容量告警。

## DTO 开发方式

HTTP 请求和响应使用普通 Java 类，以标准 getter/setter 调用，字段使用 Fastjson2 @JSONField 注解。Fastjson2 版本在父 POM 管理。Spring MVC 仍使用 Jackson，字段名称不变；本次不是全局 Fastjson 消息转换器替换。

## 本次验证

按用户要求，本次没有运行 Maven 编译、单元测试或质量检查。测试代码已更新，由开发者执行验证；此前版本的通过记录不能作为本版验证结果。
