package org.example.study.worker;

import org.example.study.detect.common.ModalityDetectRouter;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectContext;
import org.example.study.domain.DetectStatus;
import org.example.study.domain.DetectTaskMessage;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectItem;
import org.example.study.domain.DetectItemResult;
import org.example.study.baseSdk.storage.DetectionFileStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.example.study.messaging.DetectTaskQueue;
import org.example.study.baseSdk.lifecycle.ServiceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 单模态服务的同步执行和异步入队能力。
 */
@Service
public class ModalityWorkerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModalityWorkerService.class);

    private final ModalityDetectRouter router;
    private final DetectResultMapper resultMapper;
    private final DetectTaskQueue taskQueue;
    private final ServiceLifecycleManager lifecycle;
    private final DetectionFileStore fileStore;
    @org.springframework.beans.factory.annotation.Value("${study.worker.request-timeout-ms:25000}")
    private long requestTimeoutMillis = 25000;

    @jakarta.annotation.PostConstruct
    public void validateConfiguration() {
        if (requestTimeoutMillis <= 0 || requestTimeoutMillis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Request timeout must be positive and within the supported range");
        }
    }

    public ModalityWorkerService(ModalityDetectRouter router, DetectResultMapper resultMapper, DetectTaskQueue taskQueue,
                                 ServiceLifecycleManager lifecycle) {
        this(router, resultMapper, taskQueue, lifecycle, null);
    }

    @Autowired
    public ModalityWorkerService(ModalityDetectRouter router, DetectResultMapper resultMapper, DetectTaskQueue taskQueue,
                                 ServiceLifecycleManager lifecycle, DetectionFileStore fileStore) {
        this.router = router;
        this.resultMapper = resultMapper;
        this.taskQueue = taskQueue;
        this.lifecycle = lifecycle;
        this.fileStore = fileStore;
    }

    public DetectResult detect(DetectCommand command) {
        try (var lease = lifecycle.acquire()) {
            if (command.executionMode() != org.example.study.domain.DetectExecutionMode.SYNC) {
                throw new IllegalArgumentException("Synchronous detection requires synchronous execution mode");
            }
            return detectAccepted(command);
        }
    }

    /** 同一请求顺序执行独立上下文，失败项目不阻断其余项目，也不逐项发送 MQ。 */
    DetectResult detectAccepted(DetectCommand command) {
        var results = new java.util.ArrayList<DetectItemResult>();
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(requestTimeoutMillis);
        for (DetectItem item : command.items()) {
            DetectItemResult result;
            if (item.errorCode() != null && !item.errorCode().isBlank()) {
                result = DetectItemResult.degradedPass(item.itemId(), item.errorCode(), item.errorMessage());
            } else if (System.nanoTime() >= deadline) {
                result = DetectItemResult.degradedPass(item.itemId(), "DETECT_TIMEOUT", "Detection request execution deadline exceeded");
            } else {
                try {
                    String url = item.url();
                    if (item.fileId() != null) { url = fileStore.resolve(item.fileId()).toUri().toString(); }
                    DetectContext context = new DetectContext(command, item, url);
                    context.putAttribute("itemId", item.itemId());
                    context.putAttribute("language", command.language() == null ? "" : command.language());
                    context.putAttribute("region", command.region() == null ? "" : command.region());
                    var logContext = context.logContext();
                    context.setLogContext(new org.example.study.baseSdk.chain.ChainLogContext(logContext.logSpace(),
                            logContext.traceId(), logContext.taskId(), logContext.appId(), logContext.sceneCode(),
                            logContext.chainName(), logContext.chainVersion(), java.util.Map.of("itemId", item.itemId())));
                    var chainResult = java.util.Objects.requireNonNull(router.execute(command.contentType(), context), "Chain result must not be null");
                    result = chainResult.success() ? resultMapper.from(item.itemId(), chainResult)
                            : DetectItemResult.degradedPass(item.itemId(), "DETECT_CHAIN_FAILED", "Detection chain execution failed");
                } catch (Exception exception) {
                    LOGGER.error("Detection request item detection failed, taskId={}, itemId={}", command.taskId(), item.itemId(), exception);
                    result = DetectItemResult.degradedPass(item.itemId(), "DETECT_EXECUTION_FAILED", "Detection execution failed");
                }
            }
            results.add(result);
        }
        return new DetectResult(command.taskId(), command.contentType(), "COMPLETED", results);
    }

    public AsyncDetectAcceptedResult submit(DetectCommand command) {
        try (var lease = lifecycle.acquire()) {
            DetectTaskMessage message = new DetectTaskMessage("evt_" + compactUuid(), command);
            try {
                if (taskQueue.publish(message)) {
                    return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(), DetectStatus.ACCEPTED,
                            null, command.submittedAt(), true, false, "", "");
                }
            } catch (RuntimeException exception) {
                LOGGER.error("Detection request task publish failed, taskId={}", command.taskId(), exception);
            }
            return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(), DetectStatus.FAILED,
                    DetectAction.PASS, command.submittedAt(), false, true, "TASK_QUEUE_UNAVAILABLE", "Detection task was not queued");
        }
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
