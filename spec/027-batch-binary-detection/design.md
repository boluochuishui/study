# 当前设计

## HTTP 与 DTO

DetectController 仅保留 POST /api/detect/sync 和 POST /api/detect/async，均只接受 application/json。同步文本/图片固定一个样本，异步文本/图片允许多个，音视频仍只允许一个。取消直接文件上传，不提供 uploadSync/uploadAsync。

DetectRequest、DetectItemRequest、DetectResult、DetectItemResult、AsyncDetectAcceptedResult 和 ApiErrorResponse 使用普通 JavaBean 类。字段通过 Fastjson2 @JSONField(name=...) 显式映射，Lombok 生成标准 getter/setter、无参构造器和值相等逻辑，不生成包含原始样本的 toString。集合 setter 防御复制，保留默认空列表及错误字符串规范化。

内部 DetectCommand、DetectItem、DetectTaskMessage、DetectResultMessage 以及鉴权/基础模块的 record 保持不变。本次只调整 HTTP 协议 DTO，不引入全项目类型重构。Fastjson2 在父 POM dependencyManagement 统一管理，由 detect-contract 声明依赖。Spring MVC 继续使用现有 Jackson，JavaBean 属性名与 @JSONField 名保持一致；只有直接使用 Fastjson2 时该注解才决定映射，不宣称 Jackson 会自动识别它。

## 输入与校验

DetectRequest：clientRequestId、sceneCode、contentType、language、region、items。
DetectItemRequest：itemId、text、url、data。删除 filePartName。

文本使用 text；媒体使用标准 Base64 data 或 HTTP/HTTPS url。非空 data 优先，备用 URL 不校验、不执行；非法 data 生成 INVALID_SAMPLE，不回退 URL。空字符串 data 视为未提供，空白字符串为非法样本。拒绝 data URI、折行和 URL-safe 字母表。

DetectRequestValidator 先校验数量、模态、项目编号和总预算，再进行 Base64 解码、落盘及格式验证。编码大小、解码大小、实际格式和像素均受限制。有效数据转换为随机 fileId，RPC/MQ 不传原始字节。请求级协议错误返回 400，单样本内容错误保留为逐项结果。

JSON HMAC 只对原始请求字节签名，不解码、不重新序列化。删除 multipart 签名实现并关闭 Servlet multipart 支持；检测路径收到 multipart 时，鉴权入口明确返回 415。

## 执行、配置与部署

DetectService 使用原 detect/submit RPC；Worker 按顺序创建独立上下文，记录 itemId，逐项异常不阻断其余样本。一个请求对应一个 taskId、一条任务消息及一条汇总结果消息。消息 v2.0 的 results 保持顺序，存在降级项目时使用失败事件；COMPLETED 仅表示请求收尾。

输入限制仍位于 resources/detection-input.yaml，由 application.yaml 引用；同步数量固定为一。Base64 编码膨胀计入 64 MiB 总预算，单图片默认 10 MiB，单音视频默认 50 MiB；JSON 鉴权缓存默认 64 MiB。取消 multipart 后，大音视频应优先使用 URL，不能无限增加 JSON 大小。

保留 DetectionFileStore 作为 Base64 解码后的内部共享存储。接入及 Worker 必须访问同一存储；文件按 TTL 清理。Worker 请求预算、责任链超时、3 秒零重试 RPC 和弱一致性放通策略不变。

内部协议沿用此前统一 command 的版本，接入与 Worker 仍需协调升级。真实媒体下载、模型识别和集群联调不在本次修改内。用户要求自行验证，本次不运行构建或测试。
