# 验证记录

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
