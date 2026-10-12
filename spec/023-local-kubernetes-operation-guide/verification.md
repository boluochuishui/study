# 验证记录

## 文档一致性检查

- 命名空间与清单一致：`content-risk`。
- 接入端口与 Service 一致：NodePort `30080`。
- 五个本地镜像名称与部署脚本一致。
- MySQL、Redis、音视文图服务名称与当前清单一致。
- HMAC 请求字段与 `invoke-sync-detect.ps1` 一致。
- Redis 键前缀与文本服务配置一致：`study:risk:text`。

## 实际环境验证依据

022 需求执行期间已经完成以下实际验证：

- 7 个 Deployment 全部 Available。
- 7 个 Pod 全部 Running、Ready，最终重启次数为 0。
- 7 个 Service EndpointSlice 全部 Ready。
- MySQL 和 Redis PVC 全部 Bound。
- 正常文本返回 `PASS`。
- 违规文本返回 `REJECT` 和 `keyword_risk`。
- 第二次违规文本请求命中 Redis 缓存。
- 完整 Maven reactor 测试通过。
