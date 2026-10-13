# 本次状态：待开发者验证

2026-10-13 用户取消直接文件上传，并要求请求/响应使用普通类和 Fastjson2 @JSONField。代码和相关测试已修改；按用户明确要求，本次不执行 Maven 构建、测试或质量检查。

以下仅为上一版的历史验证记录，不代表当前工作区已经通过验证。

# 当前方案验证记录

## 2026-10-13 同步单样本与异步多样本修订

- 全 Maven 模块执行 clean verify，退出码 0。
- Surefire 汇总 127 项：125 项通过，0 失败，0 错误，2 项 MySQL 集成测试因 database.integration.enabled 未启用而跳过。
- Checkstyle、SpotBugs 通过；access-service 和 admin-service 的 JaCoCo 报告已生成。
- 真实 Servlet HTTP 测试：同步多样本返回 400 且不调用 RPC；同步单样本使用 results；异步两样本文本仅调用一次 submit 并返回 202。
- 验证 JSON 原始字节 HMAC 与 multipart part 签名；篡改文件或 Base64 JSON 返回 401。
- 验证有效 Base64/文件优先于 URL，包括无效备用 URL；RPC 仅携带 fileId，不携带 data。
- 验证非法 Base64、data URI、折行、损坏媒体、编码/解码/累计体积上限；失败原数据不回退 URL，后续项目继续。
- 验证请求编号、项目顺序、逐项错误、失败链状态、一个任务消息及一条汇总结果、生命周期在途租约。
- 验证统一任务消息的 JSON 往返，以及 RPC 命令对同步/音视频数量限制。
- git diff --check 通过；源码无 BatchDetect、BatchRequestPreparer、BatchDetectionProperties、detectBatch、submitBatch 及旧配置前缀。
- clean 构建后的 detect-contract、detect-worker-core、access-service Jar 中没有 Batch 类残留。
- 本次未执行真实 Kafka/Redis/MySQL、跨进程 Dubbo 或 K8s 部署联调；模态启动测试与接入集成测试通过，不代表真实模型或集群部署验证。
- RPC/内部任务消息是破坏性协议更新，发布时需统一升级接入和四个 Worker，并排空旧 Topic 或启用新 Topic。

以下为早期实现记录，仅用于追溯，不作为当前行为说明。

# 验证记录

## 2026-10-13 控制器合并

将批量 JSON 与 multipart 方法合并至 `DetectController`，删除 `BatchDetectController`。执行 `mvn -pl access-service -am -Dtest=DetectControllerTests,BatchHttpIntegrationTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过，6 项测试无失败；验证单条同步/异步、降级响应、批量 JSON、文件上传验签与异步受理。Checkstyle 通过。路径和协议保持兼容。

## 自动化验证

2026-10-12 执行全模块 mvn verify -Dstyle.color=never，BUILD SUCCESS。

- Surefire 报告合计 116 项，失败 0、错误 0、跳过 2 项既有数据库集成测试。
- 本需求新增 14 项测试：共享文件存储 3 项、批量请求准备 5 项、真实 HTTP 接入 3 项、Worker 聚合 3 项。
- 文件存储验证流式超限、空文件、读取、路径穿越、删除和 TTL 清理。
- 协议测试验证重复 itemId、数量上限、混用来源、音视频批量拒绝、未知文件字段、file:// URL 拒绝、图片格式/像素/大小限制及逐项错误保留。
- 随机端口真实 Servlet HTTP 测试验证 JSON 批量、multipart 上传、验签后文件仍可读、文件和 JSON 篡改返回 401、异步受理成功返回 202。RPC 客户端在这些 HTTP 测试中是 Mock，不是远程进程。
- Worker 测试验证整批只入队一次、只发布一条包含全部 itemId 的聚合结果，异常项目不阻断后续项目，失败责任链不会被标成正常成功。
- Checkstyle、SpotBugs、现有 JaCoCo 门禁以及所有 Jar 构建通过。共享存储和配置 Bean 引用按具体类及字段排除注入型告警；真实空值路径修复，没有排除空指针告警。
- git diff --check 无空白错误；kubectl kustomize deploy/k8s/local 成功渲染一个文件 PVC 和五个 Java 服务的共享挂载。

## 未验证边界

- Docker 当前未运行，访问 dockerDesktopLinuxEngine 失败；未构建/运行本次容器镜像。
- 当前 kubectl 无 current-context，未实际部署或验证 PVC 权限、容量和多 Pod 共享。
- 尝试启动本机两个独立 Java 进程做真实 RPC 上传联调，执行工具拒绝启动命令，测试未运行，不能声称完成跨进程端到端验证。
- 真实 Kafka 入队、Broker 结果消息、重平衡与重投递未联调；自动化测试验证的是 publish 调用次数及消息结构。
- 音视频仅做常见文件头校验，现有 OCR/ASR/视觉检测节点仍为模拟实现，不承诺上传内容有真实模型审核能力。
- 未进行大文件、大批量并发压测；项目间预算不强制中断正在运行的节点，上传存储 TTL 也不是容量配额。
