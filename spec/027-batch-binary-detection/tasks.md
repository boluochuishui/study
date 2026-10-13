# 实现任务

## 初版与此前修订（历史记录）

- [x] 契约与兼容构造器
- [x] 批量接入、校验及 multipart 验签
- [x] 共享文件存储、格式与大小限制
- [x] RPC 与逐项责任链执行
- [x] 异步单条聚合结果消息
- [x] 测试、质量门禁、操作文档
- [x] 2026-10-13 将批量和上传入口合并至 DetectController
- [x] 2026-10-13 统一同步/异步路径的 items 请求和 results 响应，删除 batch 路径并迁移脚本及测试

## 本次确认方案修订

- [x] 同步固定单样本，异步文本/图片多样本
- [x] 将能力合并回原模型、服务、校验器和 RPC
- [x] 删除 Batch 类及旧任务消息分支
- [x] 增加 Base64、原数据优先和不回退规则
- [x] resources 输入预算及共用 Worker 超时配置
- [x] 更新单元测试、真实 HTTP 验签测试和迁移说明
- [x] 全模块 clean verify 与最终检查：125 项通过，2 项 MySQL 集成测试未启用

## JSON 输入与普通 DTO 修订

- [x] 删除直接上传处理方法和 multipart 签名/校验分支
- [x] 删除 filePartName，保留 Base64 与 URL
- [x] 请求及响应 DTO 改为 JavaBean + Fastjson2 @JSONField
- [x] 父 POM 管理 Fastjson2 版本，更新调用与测试代码
- [x] 更新需求、设计及操作文档
- [ ] 开发者自行验证（用户要求，本次不运行构建和测试）
