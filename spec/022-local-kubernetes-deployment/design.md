# 设计方案

## 拓扑

```text
localhost:30080
       |
access-service
       |
       +-- text-detect-service:50051 -- Redis
       +-- image-detect-service:50052
       +-- audio-detect-service:50053
       +-- video-detect-service:50054

MySQL <--- access-service / modality services
```

所有资源位于 `content-risk` 命名空间。检测服务只暴露为 `ClusterIP`，只有接入服务使用 `NodePort 30080` 对本机开放。

## 镜像

镜像使用 `content-risk/<service>:local` 命名，并设置 `imagePullPolicy: Never`，避免本地开发镜像被错误地从远程仓库拉取。

## 配置与密钥

- 非敏感配置由 `content-risk-config` ConfigMap 提供。
- MySQL 密码、演示 App Secret 由 `deploy-local.ps1` 创建 `content-risk-local-secret`。
- 清单中只通过 `secretKeyRef` 引用密钥。

## 健康检查

- MySQL 使用 `mysqladmin ping`。
- Redis 使用 `redis-cli ping`。
- Java 服务使用 TCP socket 探针，后续引入 Actuator 后可升级为 HTTP 存活和就绪探针。

## 资源策略

本地集群为每个 Java 服务设置较小的 request/limit，并通过 `MaxRAMPercentage` 约束 JVM，避免多个微服务同时耗尽 Docker Desktop 内存。
