# 内容风控检测服务

项目包含接入服务、文本/图片/音频/视频检测服务、管理服务和 Vue 管理台，支持 Docker Desktop Kubernetes 本地部署。

## 命令执行位置

本文所有命令均在包含根 `pom.xml` 的项目目录执行：

```powershell
cd C:\Users\admin\Desktop\myproject\study\study
```

需要安装 JDK 21、Maven、Docker Desktop 和 kubectl，并在 Docker Desktop 中启用 Kubernetes。检查环境：

```powershell
java -version
mvn -version
docker version
kubectl config current-context
```

部署脚本要求当前上下文为 `docker-desktop`。前端通过 Docker 构建，无需本机额外安装 Node。

## 一键启动整个项目

首次部署或更新全部服务：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File deploy/k8s/local/deploy-local.ps1
```

脚本会打包 Java 模块、构建七个应用镜像、配置 Secret、部署 MySQL/Redis 和应用，并等待所有服务就绪。重建后自动滚动重启应用，运行最新的 `local` 标签镜像。

已构建镜像，仅需应用部署配置时：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File deploy/k8s/local/deploy-local.ps1 -SkipBuild
```

`-SkipBuild` 不主动重启配置未变化的服务，也不代替代码更新后的镜像构建。

| 入口 | 地址 | 说明 |
| --- | --- | --- |
| 管理台 | http://localhost:30088 | 真实管理 API，关闭 Mock |
| 检测接入 | http://localhost:30080 | 同步 `/api/detect/sync`，异步 `/api/detect/async` |

管理台租户为 `default`，用户名为 `admin`。首次部署生成的密码读取命令：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File deploy/k8s/local/get-admin-password.ps1
```

已有用户不会因重新部署被重置密码。这里连接的是 K8s 内的 MySQL，与 Windows 本机 MySQL 数据独立。

## 各服务单独更新部署

以下命令适用于已经完成首次一键部署的集群：数据库、Secret、PVC、Deployment 和 Service 均已存在。各段命令可独立执行，只更新对应服务。Maven 的 `-am` 会构建依赖 SDK，不会重启其他服务；这里的 `-DskipTests` 跳过单元测试，提交前可另行运行 `mvn verify`。

### 接入服务

```powershell
mvn -pl access-service -am package -DskipTests
docker build -t content-risk/access-service:local -f Dockerfile .
kubectl -n content-risk rollout restart deployment/access-service
kubectl -n content-risk rollout status deployment/access-service --timeout=180s
```

### 文本检测服务

```powershell
mvn -pl text-detect-service -am package -DskipTests
docker build -t content-risk/text-detect-service:local -f text-detect-service/Dockerfile .
kubectl -n content-risk rollout restart deployment/text-detect-service
kubectl -n content-risk rollout status deployment/text-detect-service --timeout=180s
```

### 图片检测服务

```powershell
mvn -pl image-detect-service -am package -DskipTests
docker build -t content-risk/image-detect-service:local -f image-detect-service/Dockerfile .
kubectl -n content-risk rollout restart deployment/image-detect-service
kubectl -n content-risk rollout status deployment/image-detect-service --timeout=180s
```

### 音频检测服务

```powershell
mvn -pl audio-detect-service -am package -DskipTests
docker build -t content-risk/audio-detect-service:local -f audio-detect-service/Dockerfile .
kubectl -n content-risk rollout restart deployment/audio-detect-service
kubectl -n content-risk rollout status deployment/audio-detect-service --timeout=180s
```

### 视频检测服务

```powershell
mvn -pl video-detect-service -am package -DskipTests
docker build -t content-risk/video-detect-service:local -f video-detect-service/Dockerfile .
kubectl -n content-risk rollout restart deployment/video-detect-service
kubectl -n content-risk rollout status deployment/video-detect-service --timeout=180s
```

### 管理台后端

```powershell
mvn -pl admin-service -am package -DskipTests
docker build -t content-risk/admin-service:local -f admin-service/Dockerfile .
kubectl -n content-risk rollout restart deployment/admin-service
kubectl -n content-risk rollout status deployment/admin-service --timeout=180s
```

管理服务首次启动等待接入服务 READY，以确保数据库迁移完成。

### 管理台前端

```powershell
docker build -t content-risk/study-console-web:local --build-arg VITE_APP_ENV=local-k8s ./study-console-web
kubectl -n content-risk rollout restart deployment/study-console-web
kubectl -n content-risk rollout status deployment/study-console-web --timeout=180s
```

如果 Docker Hub 不可访问，且本机已有兼容的 `node:22`（版本至少 22.12）和 `nginx:alpine`，可将第一条命令替换为：

```powershell
docker build -t content-risk/study-console-web:local --build-arg NODE_IMAGE=node:22 --build-arg NGINX_IMAGE=nginx:alpine --build-arg VITE_APP_ENV=local-k8s ./study-console-web
```

前端镜像构建时固定关闭 Mock；Nginx 通过集群内的 `admin-service:8081` 转发 API。访问地址仍为 http://localhost:30088。

手动逐条运行时，上一条命令成功后再执行下一条，避免构建失败后仍重启旧镜像。上述流程用于应用代码更新；修改 K8s 清单或配置后，应重新执行部署脚本应用配置。新增数据库迁移后，先更新接入服务并确认迁移成功，再更新依赖新表的服务。

## 只重启、停止或重新启动服务

无需重建镜像，只重启某个运行中的服务：

```powershell
kubectl -n content-risk rollout restart deployment/text-detect-service
kubectl -n content-risk rollout status deployment/text-detect-service --timeout=180s
```

临时停止某个服务，以及将已停止的服务重新启动：

```powershell
kubectl -n content-risk scale deployment/text-detect-service --replicas=0
kubectl -n content-risk scale deployment/text-detect-service --replicas=1
kubectl -n content-risk rollout status deployment/text-detect-service --timeout=180s
```

上面的停止与启动命令按需分别执行。将 `text-detect-service` 替换为以下任意 Deployment 名称即可操作对应服务：

| 服务 | Deployment 名称 |
| --- | --- |
| 接入服务 | `access-service` |
| 文本检测 | `text-detect-service` |
| 图片检测 | `image-detect-service` |
| 音频检测 | `audio-detect-service` |
| 视频检测 | `video-detect-service` |
| 管理台后端 | `admin-service` |
| 管理台前端 | `study-console-web` |
| MySQL | `mysql` |
| Redis | `redis` |

接入服务允许在下游检测服务未启动时独立运行。对应检测调用失败时，按现有策略返回降级放通信息；异步本地默认 logging 队列不会真正入队。

## 状态、日志与检测验证

```powershell
kubectl -n content-risk get pods,svc,pvc
kubectl -n content-risk logs deployment/access-service --tail=100
kubectl -n content-risk logs deployment/admin-service --tail=100
kubectl -n content-risk logs deployment/study-console-web --tail=100
```

验证同步文本检测：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File deploy/k8s/local/invoke-sync-detect.ps1 -Text "normal content"
```

NodePort 无法访问时，可在另一个终端转发前端端口，保持命令运行并打开 http://localhost:8088：

```powershell
kubectl -n content-risk port-forward service/study-console-web 8088:80
```

## Maven 找不到内部 SDK 时

若单独构建管理服务时报 `Could not find artifact org.example:base-sdk` 或 `config-data`，先确认使用项目根目录和根 `pom.xml`。推荐在同一次构建中包含依赖模块：

```powershell
mvn -pl admin-service -am package -DskipTests
```

`package` 只生成各模块的 `target` 产物，不会将内部 SDK 安装到 Maven 本地仓库。若通过 IDEA Maven 面板、`-f admin-service/pom.xml` 或进入子模块目录单独执行 Maven，需要先安装内部依赖及父 POM：

```powershell
mvn -pl base-sdk,config-data -am install -DskipTests
mvn -f admin-service/pom.xml package -DskipTests
```

其他检测服务还依赖契约和 Worker 模块。需要长期逐模块开发时，可在根目录先执行一次 `mvn install -DskipTests`；SDK 代码更新后需重新安装，或继续使用根目录的 `-am` 构建方式。

若命令行成功而 IDEA 仍报相同错误，检查 IDEA 的 Maven 配置是否使用相同的 `settings.xml` 和本地仓库路径，然后重新加载根 Maven 项目。

### 子模块找不到 Checkstyle 配置

项目根目录的 `.mvn` 是 Maven 根目录标记，需要随代码一起保留。它让从子模块执行的 Maven 命令也能找到根目录下的 Checkstyle 和 SpotBugs 配置。

若报错路径为 `admin-service/config/checkstyle/checkstyle.xml`，确认根目录存在 `.mvn/README.md` 后重新运行原命令。无需在每个子模块复制公共配置。

## 其他文档

- [本地 K8s 部署目录](deploy/k8s/local/README.md)
- [完整部署操作手册](spec/023-local-kubernetes-operation-guide/operation-guide.md)
- [管理台部署和登录说明](spec/035-console-docker-kubernetes/operation-guide.md)
- [前端本机开发说明](study-console-web/README.md)
- [SDD 需求目录](spec/README.md)
