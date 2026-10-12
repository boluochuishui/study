# 使用与部署

## 1. 接口和兼容性

旧的 /api/detect/sync、/api/detect/async 和单条 DetectRequest 保持不变。本需求新增以下路径，两者均接受 application/json 或 multipart/form-data：

| 接口 | 支持内容 | 返回 |
| --- | --- | --- |
| POST /api/detect/batch/sync | 文本、图片，1 到默认 10 项 | 一个 BatchDetectResult |
| POST /api/detect/batch/async | 文本、图片，1 到默认 100 项；音频/视频限单项 | 入队成功 202，否则明确降级响应 |

任务队列默认 logging 模式，明确返回 resultExpected=false。必须启用真实 Kafka 并成功入队，才承诺正常异步处理路径；宕机和结果发布失败仍可能没有结果。

## 2. JSON 文本批量

```json
{
  "clientRequestId": "business-001",
  "sceneCode": "comment",
  "contentType": "TEXT",
  "language": "zh-CN",
  "region": "CN",
  "items": [
    {"itemId": "one", "text": "第一条文本"},
    {"itemId": "two", "text": "第二条文本"}
  ]
}
```

统一媒体 URL 项目示例为 {"itemId":"one","url":"https://example.com/image.png"}，contentType 使用 IMAGE。媒体只接受 HTTP/HTTPS URL，不接受客户端指定 file:// 或本地路径。URL 下载与模型仍沿用现有模拟节点，未来真实下载实现必须额外限制目标地址、重定向、下载量和时间，不能把本次协议校验当作完整 SSRF 防护。

## 3. 二进制上传

metadata part 使用 application/json，内容如下；img1、img2 是实际二进制文件 part，不是 Base64，也不要求客户预先上传到存储。

```json
{
  "sceneCode": "comment",
  "contentType": "IMAGE",
  "items": [
    {"itemId": "one", "filePartName": "img1"},
    {"itemId": "two", "filePartName": "img2"}
  ]
}
```

metadata 可以是不带 filename 的 JSON part，也可以是带 filename 的 JSON 文件 part。filePartName 必须与上传字段名一致，不得重复或指向 metadata，不依赖上传顺序和原始文件名。文本只能使用内联 text，不支持上传文本文件。媒体项目可各自使用 URL 或文件，但整批 contentType 必须相同。

图片第一版仅接受静态 PNG/JPEG，检查真实格式、宽高、像素数并尝试解码；文件扩展名和声明 Content-Type 不作为类型判定依据。音频支持常见 WAV/MP3/OGG/FLAC/MP4 文件头，视频支持 MP4 容器文件头；这不等同完整音视频解码验证，检测节点仍是演示实现。

## 4. 签名规范

请求继续携带 X-App-Id、X-Timestamp、X-Nonce、X-Signature。appId 与现有凭证校验关联，metadata 不提供身份覆盖字段。

JSON 使用原始请求体字节计算签名，不重新序列化 JSON。

multipart 必须使用 multipart-v1 规范，不能沿用原始 multipart boundary 字节的 JSON 验签方法。按 part 字段名升序构造 UTF-8 字符串：

```text
multipart-v1\n
base64(name):base64(filename或空串):base64(Content-Type或空串):sha256(part实际字节，小写hex)\n
...
```

Base64 使用标准编码、不折行。每个字段名唯一；metadata 和所有文件均参与签名。Content-Type 使用实际 part 头值，包括 charset；filename 使用实际发送文件名。每行末尾包含换行。客户端可参考 MultipartSignatureBody.java 和 BatchHttpIntegrationTests.java。

上述 UTF-8 字节作为 signedBody，再使用现有 HmacSignatureService 规则：

```text
POST\n
/api/detect/batch/sync或/api/detect/batch/async\n
appId\n
timestamp\n
nonce\n
sha256(signedBody，小写hex)
```

对这段规范串执行 HMAC-SHA256，密钥为调用方 secret，输出小写 hex 写入 X-Signature。不能仅签署 metadata，文件字节发生变化时必须校验失败。

默认启动鉴权快照已增加两个批量路由。如果通过配置源替换了完整鉴权快照，必须在该快照中同步添加对应路由，不能靠启动默认规则兜底。

## 5. 聚合结果

同步结果结构为 taskId、contentType、status=COMPLETED、results；每项包含 itemId、detectStatus、action、labels、degraded、errorCode、errorMessage。保持请求项目顺序，每项都有结果，损坏文件或超时项目不会消失。

异步结果使用现有结果 Topic，eventVersion=2.0、eventType=CONTENT_DETECT_COMPLETED、status=COMPLETED，并增加 results 数组。顶层 detectStatus/action/degraded 是聚合摘要：只要有降级项目，detectStatus=FAILED；COMPLETED 不代表所有项目成功。

第一版每个批量任务只调用一次结果队列 publish，不逐项发 MQ。MQ 重投递可能再次产生聚合结果，消费方按 appId+taskId 幂等。旧 v1 事件构造方式保留，新增字段为兼容性扩展；严格 Schema 消费者需要同步升级，不能假定任意旧客户端自动兼容。

## 6. 限制与超时

| 配置 | 默认 |
| --- | --- |
| study.detect.batch.max-sync-items | 10 |
| study.detect.batch.max-async-items | 100，音视频仍限 1 |
| study.detect.batch.max-text-length | 每项 5000 个 Unicode 码点 |
| study.detect.batch.max-total-text-bytes | 整批 UTF-8 文本 256 KiB |
| study.detect.batch.max-image-bytes | 每图 10 MiB |
| study.detect.batch.max-media-bytes | 每个音视频文件 50 MiB |
| study.detect.batch.max-request-bytes | 所有 part 总量 64 MiB |
| study.detect.batch.max-image-pixels | 每图 2000 万像素 |
| spring.servlet.multipart.max-file-size | 50 MB |
| spring.servlet.multipart.max-request-size | 64 MB，包含 multipart 封装 |
| study.worker.batch-timeout-ms | 25000 |
| base-sdk.detection-files.retention | 24h |

multipart 还限制 101 个 part 和 64 KiB metadata；Tomcat 自身的 part/form 限制也可能提前拒绝请求。Servlet 超限返回 413，协议校验错误返回 400；单个文件的业务限制或格式错误返回逐项 INVALID_UPLOAD。

批量顺序执行，避免无限并发。Worker 在项目之间检查整批预算，预算耗尽的剩余项目返回 BATCH_TIMEOUT；正在运行的节点仍依赖原有责任链超时与下游超时，不保证强制中断。同步批量消费者 RPC 超时为 30 秒；修改 Worker 预算时需一起调整客户端 RPC 超时和退出预算。单条接口原有超时不变。

## 7. 文件存储与 K8s

本机所有进程设置相同 DETECT_FILE_ROOT，默认同一 java.io.tmpdir 下的 study-detect-files。RPC/MQ 只携带随机 fileId，不传文件内容、不写任务数据库。

local K8s 新增 detection-files PVC，并挂载到接入及四个 Worker 的 /data/detection-files；fsGroup 提供非 root 写入权限。本地 Docker Desktop 单节点使用 RWO，多个 Pod 在同一节点共享。多节点生产部署必须改成支持 RWX 的持久存储或实现 DetectionFileStore 对象存储适配，不能用每 Pod 独立 emptyDir。

完成后的文件不立即删除，按 TTL 清理，给消息重投递保留窗口。损坏文件校验后立即删除；未入队、模糊 RPC 超时和宕机遗留文件同样靠 TTL 回收。存储需要容量告警，TTL 不是磁盘容量配额，后续可接入对象存储生命周期策略。

重建镜像再部署，按 023/025 的部署流程执行。只渲染清单可使用 kubectl kustomize deploy/k8s/local；当前没有可用上下文时不要将渲染成功写作已完成集群部署。
