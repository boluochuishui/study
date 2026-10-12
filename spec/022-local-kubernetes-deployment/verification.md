# 验证记录

## 验收命令

```powershell
kubectl -n content-risk get pods,svc,pvc
kubectl -n content-risk get endpointslices
kubectl -n content-risk logs deployment/access-service
```

## 结果

- Maven reactor 的 11 个模块打包成功，Checkstyle 无违规。
- 五个本地业务镜像构建成功。
- 接入服务、音视文图四个检测服务、MySQL、Redis 共 7 个 Pod 全部 Ready。
- MySQL 与 Redis PVC 均通过 Docker Desktop `hostpath` 存储类绑定。
- HMAC 签名的普通文本同步检测返回 `PASS`。
- HMAC 签名的违规文本同步检测返回 `REJECT` 和 `keyword_risk`。
- 相同违规样本再次检测命中 Redis，责任链日志记录 `cached violation matched`。
- Redis 中生成 `study:risk:text:v1:*` 缓存键。
- 接入服务通过 `tri://text-detect-service:50051` 完成集群内 RPC 调用。
- `mvn test` 完整 reactor 构建成功，所有模块测试无失败、无错误。
- 增加 MySQL init container 后，重新创建的接入服务 Pod 启动次数为 0。
