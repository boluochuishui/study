# 验证记录

## 自动化检查

- 执行全模块 `mvn test`，随后补跑生命周期定向测试；最终 Surefire 报告共 100 项，失败 0、错误 0、跳过 2 项数据库集成测试。
- 定向测试覆盖启动准入、初始化失败、租约幂等释放、在途等待、超时收尾、异常钩子、HTTP 令牌及来源限制、真实 HTTP 摘流、Kafka 容器生命周期、检测任务与结果发送之间的收尾顺序。
- 执行 `mvn verify -DskipTests`：全模块构建成功，Checkstyle 无违规，SpotBugs 无告警，Jar 打包和现有 JaCoCo 门禁通过。本次 verify 使用上述测试生成的覆盖率数据，没有再次运行测试。
- Kafka 注册表由 Spring 管理，其共享引用产生的 SpotBugs 告警按具体类、字段和告警类型排除，没有关闭整体检查。
- `kubectl kustomize deploy/k8s/local` 成功渲染；五个 Java Deployment 均包含 HTTP 探针、滚动发布参数、摘流钩子和 120 秒退出宽限。

## Docker 实测

使用已缓存的 Java 21 基础镜像构建独立的 `lifecycle-test` 标签，不替换原有本地部署镜像。

1. 接入服务：启动后 ready 返回 200/READY；从宿主机发送带正确令牌的摘流请求仍返回 403，证明来源限制生效。
2. 接入服务：在容器内部发送带令牌的摘流请求，返回 DRAINING；ready 返回 503，live 返回 200，业务接口返回 503。
3. 检测 Worker：图片服务完成 RPC 初始化后 ready 返回 200；直接执行 `docker stop -t 60`，日志出现 SERVICE_DRAINING、SERVICE_STOPPED 和 Tomcat 优雅关闭完成。
4. 两个实例的 SERVICE_STOPPED 均记录 unfinishedTasks=0。Docker 退出码均为 143，表示收到 SIGTERM；结合收尾日志确认不是超时后的 SIGKILL。未进行真实长时间音视频检测压测，在途等待由自动化测试验证。
5. 临时容器已清理，原有容器和镜像标签未修改。

## 未验证范围

- 当前 `kubectl config current-context` 返回 current-context is not set，因此没有实际部署到 Kubernetes，也没有验证集群滚动更新及端点传播时间。
- Kafka 生命周期使用模拟容器验证，Docker 实测采用项目默认 logging 模式；真实 Kafka 重平衡、重投递与结果发送超时需接入 Broker 后验证。
- Docker 使用默认直连 RPC / N/A 注册中心；真实 Nacos 等注册中心的注销传播尚未验证。
- 宕机、SIGKILL、不响应中断的任务和超出总退出预算的任务，不保证检测完成或结果必达。
