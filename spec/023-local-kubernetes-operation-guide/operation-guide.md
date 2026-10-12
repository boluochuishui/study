# 本地 Kubernetes 部署操作手册

## 1. 部署目标

本地环境会把内容风控检测运行面部署到 Docker Desktop 自带的单节点 Kubernetes 集群中。

```text
本机客户端
    |
    | HTTP :30080
    v
access-service
    |
    | Dubbo Triple RPC
    +-- text-detect-service:50051 -- Redis
    +-- image-detect-service:50052
    +-- audio-detect-service:50053
    +-- video-detect-service:50054

access-service、检测服务 ---- MySQL
```

本次部署包含：

| 组件 | 用途 | 暴露方式 |
| --- | --- | --- |
| `access-service` | HTTP 接入、鉴权、任务分发 | NodePort `30080` |
| `text-detect-service` | 文本同步和异步检测 | ClusterIP `50051` |
| `image-detect-service` | 图片同步和异步检测 | ClusterIP `50052` |
| `audio-detect-service` | 音频异步检测 | ClusterIP `50053` |
| `video-detect-service` | 视频异步检测 | ClusterIP `50054` |
| MySQL | 配置项、鉴权规则、SpEL 规则等数据 | 仅集群内部访问 |
| Redis | 文本违规结果缓存 | 仅集群内部访问 |

本地版本暂不部署 Nacos、Kafka 和管理台服务。Dubbo 使用 Kubernetes Service DNS 直连，异步消息使用项目的 `logging` 模式。

## 2. 认识部署文件

部署文件位于 `deploy/k8s/local`：

| 文件 | 作用 |
| --- | --- |
| `namespace.yaml` | 创建独立的 `content-risk` 命名空间 |
| `configmap.yaml` | 保存数据库地址、Redis 地址和功能开关等非敏感配置 |
| `infrastructure.yaml` | 部署 MySQL、Redis、Service 和 PVC |
| `services.yaml` | 部署接入服务和音视文图检测服务 |
| `kustomization.yaml` | 汇总全部 Kubernetes 清单 |
| `deploy-local.ps1` | 自动编译、构建镜像、创建 Secret 并部署 |
| `invoke-sync-detect.ps1` | 自动生成 HMAC-SHA256 签名并请求文本同步检测接口 |

敏感信息不直接写在 YAML 中。`deploy-local.ps1` 会在本地集群中动态创建 `content-risk-local-secret`。

## 3. 第一次部署前检查

### 3.1 启动 Docker Desktop

打开 Docker Desktop，等待左下角显示 `Engine running`。进入 Kubernetes 页面，确认 Kubernetes 已启用并处于 Running 状态。

建议为 Docker Desktop 分配至少 6 GB 内存。本项目会同时运行 Kubernetes 系统组件、5 个 Java 服务、MySQL 和 Redis，内存过低容易导致 Pod 被系统终止。

### 3.2 检查 Docker Engine

目的：确认命令行可以连接 Docker Desktop 的 Linux Engine。

```powershell
docker version
```

成功时输出同时包含 `Client` 和 `Server`。如果只有 Client 或出现连接错误，说明 Docker Engine 尚未启动完成。

### 3.3 检查 kubectl 和当前集群

目的：防止把本地清单部署到其他 Kubernetes 集群。

```powershell
kubectl version --client
kubectl config current-context
kubectl get nodes
```

预期结果：

- 当前上下文为 `docker-desktop`。
- 存在名为 `docker-desktop` 的节点。
- 节点状态为 `Ready`。

如果上下文不是 `docker-desktop`，执行：

```powershell
kubectl config use-context docker-desktop
```

### 3.4 进入项目根目录

后续命令默认在包含根 `pom.xml` 的目录执行：

```powershell
cd C:\Users\admin\Desktop\myproject\study\study
Test-Path .\pom.xml
```

预期输出为 `True`。

## 4. 执行首次一键部署

### 4.1 使用默认本地凭证部署

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/local/deploy-local.ps1
```

脚本会依次完成：

1. 检查当前 Kubernetes 上下文是否为 `docker-desktop`。
2. 执行 `mvn package -DskipTests`，生成各服务 Jar。
3. 构建五个 `content-risk/*:local` Docker 镜像。
4. 创建 `content-risk` 命名空间。
5. 创建或更新 `content-risk-local-secret`。
6. 应用 ConfigMap、PVC、Service 和 Deployment。
7. 等待 MySQL、Redis 和五个业务服务全部就绪。
8. 输出 Pod、Service 和 PVC 的最终状态。

默认开发凭证为：

```text
MySQL root password: abc123456
App ID:              demo-app
App Secret:          demo-secret
```

这些值只适用于本机开发，不能用于生产环境。

### 4.2 使用自定义本地凭证部署

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/local/deploy-local.ps1 `
  -MysqlPassword "your-local-mysql-password" `
  -AuthAppId "your-app-id" `
  -AuthAppSecret "your-app-secret"
```

调用验证脚本时必须传入相同的 App ID 和 App Secret。

### 4.3 已有镜像时跳过构建

只有确认五个本地镜像已经存在且内容正确时才使用：

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/local/deploy-local.ps1 -SkipBuild
```

可以先检查镜像：

```powershell
docker image ls "content-risk/*"
```

## 5. 判断部署是否成功

### 5.1 检查 Deployment

```powershell
kubectl -n content-risk get deployments
```

预期有 7 个 Deployment，并且每一行的 `READY`、`UP-TO-DATE` 和 `AVAILABLE` 都为 `1`。

### 5.2 检查 Pod

```powershell
kubectl -n content-risk get pods -o wide
```

成功标志：

- 7 个 Pod 的 `READY` 都是 `1/1`。
- `STATUS` 都是 `Running`。
- 稳定运行后 `RESTARTS` 不再增长。

### 5.3 检查 Service 和内部地址

```powershell
kubectl -n content-risk get services
kubectl -n content-risk get endpointslices
```

`access-service` 应显示 `8080:30080/TCP`。每个 EndpointSlice 都应存在 IP，并且对应端点为 Ready。这说明 Service 已经选中了正确的 Pod。

### 5.4 检查持久卷

```powershell
kubectl -n content-risk get pvc
```

`mysql-data` 和 `redis-data` 都应为 `Bound`。Docker Desktop 默认使用 `hostpath` StorageClass 保存数据。

## 6. 调用文本同步检测接口

接口为：

```text
POST http://localhost:30080/api/detect/sync
```

接口需要以下鉴权头：

- `X-App-Id`
- `X-Timestamp`
- `X-Nonce`
- `X-Signature`

签名原文格式为：

```text
HTTP方法\n请求路径\nAppId\n时间戳\n随机数\n请求体SHA256
```

再使用 App Secret 计算 HMAC-SHA256。验证脚本已经封装这套逻辑。

### 6.1 验证正常文本

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/local/invoke-sync-detect.ps1 `
  -Text "normal content"
```

预期结果包含：

```text
detectStatus : SUCCEEDED
action       : PASS
degraded     : False
```

### 6.2 验证违规文本

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/local/invoke-sync-detect.ps1 `
  -Text "这是赌博内容"
```

预期结果包含：

```text
detectStatus : SUCCEEDED
action       : REJECT
labels       : {keyword_risk}
```

### 6.3 使用自定义鉴权凭证请求

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/local/invoke-sync-detect.ps1 `
  -AppId "your-app-id" `
  -AppSecret "your-app-secret" `
  -Text "normal content"
```

每次调用都会生成新的 `clientRequestId`、时间戳和 nonce，因此不会被重放攻击校验拒绝。

## 7. 验证内部链路

### 7.1 验证 HTTP 到 Dubbo RPC

成功调用文本接口已经说明 access-service 可以通过以下地址访问文本检测服务：

```text
tri://text-detect-service:50051
```

查看双方日志：

```powershell
kubectl -n content-risk logs deployment/access-service --tail=100
kubectl -n content-risk logs deployment/text-detect-service --tail=200
```

文本服务日志中应出现对应 `taskId`、`traceId`、`CHAIN_START` 和 `CHAIN_END`。

### 7.2 验证 MySQL 和 Flyway

```powershell
kubectl -n content-risk exec deployment/mysql -- sh -c `
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -D "$MYSQL_DATABASE" -e "SHOW TABLES;"'
```

预期至少能看到：

- `flyway_schema_history`
- `sdk_config_item`
- `sdk_spel_rule_set`
- `admin_user`
- `admin_role`
- `admin_permission`

存在 `flyway_schema_history` 说明 access-service 启动时已执行数据库版本迁移。

### 7.3 验证 Redis 违规缓存

先连续执行两次相同的违规文本检测，再查询缓存键：

```powershell
kubectl -n content-risk exec deployment/redis -- `
  redis-cli --scan --pattern "study:risk:text:*"
```

预期至少返回一个以 `study:risk:text:v1:` 开头的键。

继续检查第二次请求是否命中缓存：

```powershell
kubectl -n content-risk logs deployment/text-detect-service --tail=300 | `
  Select-String "cached violation matched"
```

命中缓存后责任链会提前结束，不再重复执行后续关键词和正则节点。

## 8. 修改代码后的重新部署

### 8.1 重新编译和构建镜像

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/local/deploy-local.ps1
```

本地镜像固定使用 `:local` 标签。Kubernetes Deployment 模板未变化时，不会仅因为同名镜像内容变化就自动新建 Pod，因此代码更新后还需要执行滚动重启。

### 8.2 滚动重启业务服务

只修改某个服务时，仅重启对应 Deployment。例如修改文本服务：

```powershell
kubectl -n content-risk rollout restart deployment/text-detect-service
kubectl -n content-risk rollout status deployment/text-detect-service --timeout=180s
```

修改公共模块或 RPC 契约时，重启全部业务服务：

```powershell
kubectl -n content-risk rollout restart deployment/access-service
kubectl -n content-risk rollout restart deployment/text-detect-service
kubectl -n content-risk rollout restart deployment/image-detect-service
kubectl -n content-risk rollout restart deployment/audio-detect-service
kubectl -n content-risk rollout restart deployment/video-detect-service
kubectl -n content-risk get pods -w
```

观察到新 Pod 全部 `Running` 后，按 `Ctrl+C` 退出持续观察。

### 8.3 查看和回滚发布

```powershell
kubectl -n content-risk rollout history deployment/text-detect-service
kubectl -n content-risk rollout undo deployment/text-detect-service
kubectl -n content-risk rollout status deployment/text-detect-service --timeout=180s
```

注意：当前本地镜像复用 `:local` 标签，Kubernetes 的 Deployment 回滚只恢复资源模板，不能恢复已经被同名覆盖的旧镜像内容。需要可靠镜像回滚时，应改用不可变版本标签，例如 Git commit SHA。

## 9. 日志与故障排查

建议始终按“Pod 状态 -> 事件 -> 当前日志 -> 上一次日志 -> Service 端点”的顺序排查。

### 9.1 查看 Pod 状态和事件

```powershell
kubectl -n content-risk get pods
kubectl -n content-risk describe pod <pod-name>
kubectl -n content-risk get events --sort-by=.lastTimestamp
```

`describe` 输出底部的 Events 通常会直接说明镜像、探针、调度或挂载失败的原因。

### 9.2 查看当前和崩溃前日志

```powershell
kubectl -n content-risk logs deployment/access-service --tail=200
kubectl -n content-risk logs deployment/access-service --previous --tail=200
kubectl -n content-risk logs deployment/text-detect-service -f
```

`-f` 会持续输出日志，按 `Ctrl+C` 退出。

### 9.3 `ErrImageNeverPull` 或 `ImagePullBackOff`

业务镜像配置了 `imagePullPolicy: Never`，必须存在于 Docker Desktop 本地镜像仓库。

```powershell
docker image ls "content-risk/*"
```

如果镜像缺失，重新运行不带 `-SkipBuild` 的部署脚本。

### 9.4 `CrashLoopBackOff`

先查看上一次启动日志：

```powershell
kubectl -n content-risk logs deployment/access-service --previous --tail=300
```

常见原因包括 MySQL 连接失败、Secret 与已有数据库密码不一致、配置项错误或 Jar 启动异常。

### 9.5 MySQL 或 Redis 一直未就绪

```powershell
kubectl -n content-risk describe pod -l app=mysql
kubectl -n content-risk logs deployment/mysql --tail=200
kubectl -n content-risk describe pod -l app=redis
kubectl -n content-risk logs deployment/redis --tail=200
kubectl -n content-risk get pvc
```

重点检查镜像拉取、PVC 是否 Bound、Docker Desktop 磁盘空间和容器内存。

### 9.6 `localhost:30080` 无法访问

```powershell
kubectl -n content-risk get service access-service
kubectl -n content-risk get endpointslices -l kubernetes.io/service-name=access-service
Test-NetConnection localhost -Port 30080
```

如果 Service 和 Endpoint 正常但 NodePort 仍不可访问，可以临时使用端口转发：

```powershell
kubectl -n content-risk port-forward service/access-service 8080:8080
```

保持该窗口运行，在另一个终端把验证脚本的地址改为：

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/local/invoke-sync-detect.ps1 `
  -Endpoint "http://localhost:8080/api/detect/sync"
```

### 9.7 HMAC 请求返回 401

检查以下内容：

1. App ID 和 App Secret 是否与部署时传入的值一致。
2. 本机时间是否准确，时间戳偏差过大会被拒绝。
3. nonce 是否被重复使用。
4. 签名时使用的请求体字节是否与真正发送的请求体完全一致。

开发环境可以检查 Secret 的键名，但不要在共享终端或日志中输出 Secret 明文：

```powershell
kubectl -n content-risk describe secret content-risk-local-secret
```

## 10. 暂停、恢复和删除

### 10.1 暂停服务并保留 PVC 数据

```powershell
kubectl -n content-risk scale deployment --all --replicas=0
```

这会停止所有业务服务、MySQL 和 Redis，但保留 Deployment、Service、Secret 和 PVC。

恢复：

```powershell
kubectl -n content-risk scale deployment --all --replicas=1
kubectl -n content-risk get pods -w
```

### 10.2 只删除业务服务并保留 MySQL、Redis

```powershell
kubectl delete -f deploy/k8s/local/services.yaml
```

重新创建业务服务：

```powershell
kubectl apply -f deploy/k8s/local/services.yaml
```

### 10.3 彻底删除本地环境

警告：下面的命令会删除整个命名空间，并删除 MySQL、Redis PVC。由于本地 StorageClass 的回收策略为 `Delete`，数据库和缓存数据也会一并丢失。

```powershell
kubectl delete namespace content-risk
```

确认已删除：

```powershell
kubectl get namespace content-risk
```

如果不再需要本地业务镜像，可以单独删除：

```powershell
docker image rm content-risk/access-service:local
docker image rm content-risk/text-detect-service:local
docker image rm content-risk/image-detect-service:local
docker image rm content-risk/audio-detect-service:local
docker image rm content-risk/video-detect-service:local
```

## 11. 生产化前仍需补充的能力

当前清单适合本机学习与联调，不能直接作为生产清单。生产环境至少还需要：

- 使用镜像仓库和不可变版本标签，不使用 `:local`。
- 使用 Secret 管理系统，不在命令行传递正式密钥。
- 为 Java 服务增加 Actuator HTTP 存活、就绪和启动探针。
- 使用 Ingress 或 API Gateway 替代 NodePort。
- 使用高可用 MySQL、Redis，并建立备份恢复方案。
- 接入 Kafka、Nacos 或生产选定的消息与注册中心方案。
- 增加 PodDisruptionBudget、反亲和、HPA 和多副本配置。
- 接入 Prometheus、日志平台和告警系统。
- 配置 NetworkPolicy、RBAC 和镜像安全扫描。
