# 优雅上下线操作说明

## 1. 默认配置

所有六个 Java 服务在 application.yaml 中引入 `classpath:service-lifecycle.yaml`。工作线程池使用原有 `base-sdk.thread-pool` 配置。

| 环境变量 | 默认值 | 含义 |
| --- | --- | --- |
| SERVICE_DRAIN_TIMEOUT | 40s | 应用在途任务与资源收尾共用的预算 |
| SHUTDOWN_PHASE_TIMEOUT | 45s | Spring 每个生命周期阶段的等待时间 |
| SERVICE_DRAIN_TOKEN | 空 | 手工摘流令牌，为空时禁止手工摘流 |

K8s Pod 总退出宽限为 120 秒，preStop 最多 5 秒网络等待，再等待 5 秒传播。根据实际音视频最大任务时长调整这些值，不能只修改应用预算而保持较短的 Pod 宽限。

## 2. 应用端点

端口为对应服务的 HTTP 端口：接入层与检测 Worker 默认 8080，管理台默认 8081。

| 方法及路径 | 行为 |
| --- | --- |
| GET /internal/lifecycle/live | 进程可响应时返回 200，不随 Redis 短暂故障重启 |
| GET /internal/lifecycle/ready | READY 返回 200，其余状态返回 503 |
| POST /internal/lifecycle/drain | 实际客户端必须是 loopback，携带 X-Drain-Token；摘流成功返回 200 |

摘流后 `/api/*` 返回 503 和 SERVICE_UNAVAILABLE，存活探针继续返回 200，等待部署系统终止。重复摘流没有副作用；摘流之后恢复业务必须重建实例。RPC 拒绝新任务，由接入服务沿用故障放通策略处理。

## 3. 构建和发布

先执行质量门禁：

```powershell
mvn verify
```

本地集群需已开启 Docker Desktop Kubernetes，且当前上下文为 docker-desktop：

```powershell
kubectl config current-context
powershell -ExecutionPolicy Bypass -File deploy/k8s/local/deploy-local.ps1
```

部署脚本生成随机 SERVICE_DRAIN_TOKEN，和其他本地凭证一起写入 Secret。不要在生产环境使用演示凭证。只执行 `kubectl apply -k` 之前必须确保 Secret 已有新令牌键。

现有本地镜像使用 local 标签。同名镜像重新构建后，需要主动触发 Deployment 更新才会创建新 Pod：

```powershell
kubectl -n content-risk rollout restart deployment/text-detect-service
kubectl -n content-risk rollout status deployment/text-detect-service --timeout=180s
kubectl -n content-risk get pods -w
```

生产环境应使用不可变版本标签，通过更新镜像触发滚动发布，观察新实例 READY 后再替换旧实例。当前 maxUnavailable=0、maxSurge=1，需要额外资源启动临时副本。

## 4. 验证上线

```powershell
kubectl -n content-risk exec deployment/text-detect-service -- wget -q -O - http://127.0.0.1:8080/internal/lifecycle/ready
kubectl -n content-risk logs deployment/text-detect-service --tail=100
```

预期健康结果为 READY，启动日志有 SERVICE_READY。故意让已注册的首次规则快照加载失败时，实例不得返回 READY。没有注册热加载模块时，不会强制连接数据库；已注册模块都必须初始化成功。

## 5. 验证下线

优先用滚动发布验证 preStop。独立摘流测试会让被测 Pod 持续不可用，应只对测试实例执行：

```powershell
kubectl -n content-risk exec deployment/text-detect-service -- sh -c 'wget -q -O - --header="X-Drain-Token: $SERVICE_DRAIN_TOKEN" --post-data="" http://127.0.0.1:8080/internal/lifecycle/drain'
```

预期 READY 变为 DRAINING，readiness 返回 503、liveness 返回 200。没有令牌、错误令牌、其他 Pod 发出的摘流请求返回 403。不要把令牌打印到日志，也不要依赖伪造 X-Forwarded-For 绕过本机限制。

收到 SIGTERM 后日志应依次出现 SERVICE_DRAINING 和 SERVICE_STOPPED；超时出现 SERVICE_DRAIN_TIMEOUT，携带未完成任务数量。线程池超时会记录 Forced managed thread pool shutdown。业务线程不保证响应中断，Pod 最终宽限超时由 K8s 终止进程。

## 6. Kafka 模式

本地默认 DETECT_MESSAGING_MODE=logging，不存在真实 Kafka 消费。接入 Kafka 时设置 bootstrap 地址、各模态任务 Topic 和消费组，结果 Topic 默认 detect-result。

消费者不会在初始化期间处理任务；READY 后启动。摘流暂停当前容器，新收到但未获准入的消息保留偏移量，容器停止后可由其他实例重新处理。已接收任务继续执行责任链并等待结果 MQ 发送确认，失败沿用完整日志记录、允许结果丢失的策略。

调大 `study.worker.kafka.max-poll-interval-ms` 时需结合最长检测时间，避免长任务触发消费组重平衡。生产者 max.block.ms 和发送确认等待默认使用 publish-timeout=3s。结果 MQ 仍可能重复，消费者需要按事件或任务做幂等。

## 7. 管理台部署

当前 local K8s 清单只部署检测运行面，尚不包含管理台。管理台已引入相同生命周期模块，在未来部署清单中使用 8081 端口探针及 preStop，并注入 SERVICE_DRAIN_TOKEN。

SIGKILL、机器宕机、超过截止时间的强制终止不属于可保证完成的正常退出流程。当前代码不提供所有任务必达或所有停机零错误的承诺。
