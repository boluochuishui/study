# 验证记录

- 已通过 PowerShell 5.1 运行一键脚本，并检查 PowerShell 7 语法；中文脚本使用 UTF-8 BOM 兼容 Windows PowerShell。
- 已通过 `kubectl apply --dry-run=client -k deploy/k8s/local` 清单检查；工作流 YAML 通过本地 YAML 解析器检查。
- 已构建七个镜像并部署，九个工作负载（七个应用及 MySQL、Redis）全部 READY。
- 已通过 NodePort 30088 验证 `/healthz` 和 `/access-config` 的 HTTP 200，确认 SPA 路由可返回页面。
- 已通过前端 Nginx 登录真实管理服务，确认当前租户为 default，用户名为 admin；六类 `/api/admin/managed/*` 查询均返回 SUCCESS。
- 已再次执行 `-SkipBuild` 部署，确认资源未变化、引导密码保持一致、所有工作负载仍 READY。

## 联调修正

- Docker Hub token 连接超时。本地使用缓存的 Node 22.20.0 和 Nginx 镜像完成构建；脚本自动优先选择已有兼容标签，标准 Dockerfile 默认仍为 Node 24 Alpine。
- 管理服务最初继承共享 ConfigMap 的 `/data/detection-files`，非 root 用户无法创建目录。清单改为管理服务独立的 `/tmp/admin-detection-files`，启动与登录验证通过。

GitHub 托管流水线尚未触发，需要提交后执行；本机 Maven 打包已通过 Checkstyle，部署验证没有重新执行 Java 全量单元测试。
