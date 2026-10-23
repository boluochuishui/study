# 管理台 Docker/K8s 操作说明

## 1. 一键部署

在包含根 `pom.xml` 的项目目录打开 PowerShell。Docker Desktop 和其 Kubernetes 应已启动，当前 kubectl 上下文应为 `docker-desktop`。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File deploy/k8s/local/deploy-local.ps1
```

脚本依次完成：Java 打包、七个镜像构建、Secret 配置、资源部署和应用滚动更新。首次前端构建需要下载 Node/Nginx 镜像和 npm 依赖。

若已有兼容基础镜像，脚本会优先复用缓存。本机本次实际使用 Node 22.20.0 和缓存的 Nginx，已完成前端构建。无缓存且 Docker Hub 连接失败时，需要恢复镜像网络连接或准备兼容镜像，再重新执行脚本。

管理服务等待接入服务完成数据库迁移，再初始化默认租户管理员。已有 MySQL PVC 不会因这条部署命令被删除。

## 2. 登录控制台

访问 `http://localhost:30088`，租户填 `default`，用户名填 `admin`。执行以下命令读取首次部署生成的密码：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File deploy/k8s/local/get-admin-password.ps1
```

该命令会在终端显示本地 Secret 中的引导密码。若数据库已有用户，初始化程序不会重设密码，需要使用原有登录密码。`-AdminBootstrapPassword` 参数只用于首次引导，不是密码重置接口；重复部署通常无需传此参数。

这里的前端关闭 Mock，管理操作写入集群 MySQL。集群 MySQL 和 Windows 本机 `localhost:3306` 的 MySQL 是两套数据库，数据不会自动同步。

## 3. 查看状态

```powershell
kubectl -n content-risk get pods,svc
kubectl -n content-risk logs deployment/admin-service
kubectl -n content-risk logs deployment/study-console-web
```

前端 Service 应显示 `80:30088/TCP`。管理服务内部端口为 8081，不需要单独在浏览器访问。

如果本机不能通过 NodePort 访问，可在另一个终端执行：

```powershell
kubectl -n content-risk port-forward service/study-console-web 8088:80
```

保持终端运行，并访问 `http://localhost:8088`。

## 4. 常见情况

- 页面出现 502：检查管理服务是否 READY、Service 是否有可用 Pod。
- 管理服务停在 initContainer：接入服务尚未 READY；检查接入日志和数据库迁移。
- `ErrImageNeverPull`：缺少本地镜像，使用不带 `-SkipBuild` 的一键命令。
- 修改代码后页面未更新：重新执行一键部署；脚本会重建镜像并滚动重启。
- 异步在线测试降级：本地仍是 logging 队列模式；加入前端不会自动启用 Kafka。
- 六表配置保存成功但检测规则未变化：需求 034 尚未实现这些新表的运行期加载。
