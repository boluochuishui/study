# 本地 Kubernetes 部署

本目录用于把检测运行面部署到 Docker Desktop Kubernetes，包括接入服务、音视文图四个检测服务、MySQL 和 Redis。服务间 RPC 通过 Kubernetes Service DNS 直连；本地环境暂不启用 Nacos、Kafka、大模型检测和 SpEL 规则热加载。

首次使用、更新发布、验证和排障的完整说明请阅读 [023 本地 Kubernetes 部署操作手册](../../../spec/023-local-kubernetes-operation-guide/operation-guide.md)。

服务就绪检查、摘流、滚动发布和停机验证见 [025 优雅上下线操作说明](../../../spec/025-graceful-service-lifecycle/operation-guide.md)。

## 一键部署

在项目根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/local/deploy-local.ps1
```

脚本会执行 Maven 打包、构建五个本地镜像、创建本地 Secret、应用清单并等待所有 Deployment 就绪。已有镜像时可增加 `-SkipBuild`。

## 验证文本检测

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/local/invoke-sync-detect.ps1 `
  -Text "normal content"
```

接入地址为 `http://localhost:30080`，默认本地凭证为 `demo-app/demo-secret`。凭证只由部署脚本写入 Kubernetes Secret，不保存在清单中。

## 运维命令

```powershell
kubectl -n content-risk get pods,svc,pvc
kubectl -n content-risk logs deployment/access-service
kubectl -n content-risk describe pod <pod-name>
kubectl delete namespace content-risk
```
