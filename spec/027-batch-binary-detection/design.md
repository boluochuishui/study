# 设计

## 协议

新增 /api/detect/batch/sync 和 /api/detect/batch/async，分别支持 JSON 与 multipart。multipart metadata 为 JSON，项目 filePartName 指向同名文件字段。文本项目使用 text，媒体 URL 项目使用 url；三种来源互斥，拒绝未绑定文件与重复文件字段。

请求字段为 clientRequestId、sceneCode、contentType、language、region、items。items 每项为 itemId、text、url、filePartName。仅内部命令允许 fileId，客户不可指定本地路径。语言与地区先携带到责任链上下文，不自动改变已有规则或识别语种。

## 执行和消息

RPC 新增 detectBatch/submitBatch。使用一个父 taskId，每项独立上下文并附加 itemId；第一版顺序执行以提供固定资源上界，不使用无限并发。沿用已有责任链单项超时及 RPC 截止时间，不宣称具备任务强制中断。

结果包含 taskId、contentType、status=COMPLETED 和 results。COMPLETED 表示处理结束，每项有独立 DetectResult。异步内部队列消息扩展可选 batch 字段，保持旧消息兼容，外部结果消息 v2.0 增加 results；整批只调用一次结果发送。

## 文件和安全

文件保存到可替换的共享存储接口，第一版实现共享文件系统；RPC/MQ 只传随机 fileId，不传大文件字节。各服务必须挂载同一目录，多节点生产部署需要 RWX 存储或后续对象存储实现，不能使用每个 Pod 独立临时目录。

保存时限制流式读取字节数、禁止路径穿越、不使用原始文件名作为路径。图片检查真实格式和宽高，拒绝超像素图片；音视频检查常见文件头，实际解码算法仍为占位节点。文件保留期限通过配置控制，周期清理过期文件；重投递可在保留期限内重新读取，不在第一次完成后立即删除。

multipart 验签采用规范化 part 清单：按字段名排序，字段名、文件名及内容类型使用 Base64 编码，附带每个 part 的 SHA-256；列表 UTF-8 字节作为原有 HMAC 签名的 body。所有 metadata 和文件字节均受签名保护。解析使用 Servlet part 缓存，不消耗原始流后再要求容器重复解析。此规则只影响新增 multipart 调用，旧 JSON 原始字节签名不变。

## 弱一致性

logging 模式保持明确降级，不伪装异步成功。任务入队失败/模糊 RPC 超时保留上传文件至 TTL，以避免删掉可能已接受的任务输入。服务宕机或聚合结果发送失败可能没有结果，遵循既有放通策略并记录异常。
