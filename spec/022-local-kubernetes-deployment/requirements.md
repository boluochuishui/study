# 022 本地 Kubernetes 部署

## 背景

项目已经拆分为接入服务和音视文图检测服务，需要在 Docker Desktop Kubernetes 中形成可重复部署、可验证的本地运行环境。

## 需求

1. 构建接入服务以及音视文图检测服务的本地 Docker 镜像。
2. 在独立命名空间中部署 MySQL、Redis 和五个检测运行面服务。
3. 接入服务通过 Kubernetes Service DNS 直连四个 Dubbo 检测服务。
4. MySQL 和 Redis 使用持久卷，业务服务设置资源限制和 TCP 探针。
5. 本地密码通过部署脚本动态创建 Kubernetes Secret，不写入部署清单。
6. 接入服务通过 NodePort 暴露给本机调用。
7. 提供构建、部署、等待就绪和状态检查脚本。
8. 完成集群内 RPC 与外部 HTTP 检测接口验证。

## 本地环境约束

- Kubernetes 上下文为 `docker-desktop`。
- 使用 Docker Desktop `kubeadm` 集群，共享本机 Docker 镜像。
- Kafka 使用项目现有 `logging` 模式，当前部署不包含 Kafka。
- RPC 使用直连地址，当前部署不包含 Nacos。
