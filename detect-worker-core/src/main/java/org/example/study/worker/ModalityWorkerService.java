package org.example.study.worker;

import org.example.study.detect.common.ModalityDetectRouter;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectContext;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectStatus;
import org.example.study.domain.DetectTaskMessage;
import org.example.study.domain.BatchDetectCommand;
import org.example.study.domain.BatchDetectResult;
import org.example.study.domain.BatchDetectItem;
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
    @org.springframework.beans.factory.annotation.Value("${study.worker.batch-timeout-ms:25000}")
    private long batchTimeoutMillis = 25000;

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

    public BatchDetectResult detectBatch(BatchDetectCommand command) {
        try (var lease = lifecycle.acquire()) { return detectBatchAccepted(command); }
    }

    /** 同一请求顺序执行独立上下文，失败项目不阻断其余项目，也不逐项发送 MQ。 */
    BatchDetectResult detectBatchAccepted(BatchDetectCommand batch) {
        var results = new java.util.ArrayList<DetectItemResult>();
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(batchTimeoutMillis);
        for (BatchDetectItem item : batch.items()) {
            DetectResult result;
            if (item.errorCode() != null && !item.errorCode().isBlank()) {
                result = DetectResult.degradedPass(batch.taskId(), item.errorCode(), item.errorMessage());
            } else if (System.nanoTime() >= deadline) {
                result = DetectResult.degradedPass(batch.taskId(), "BATCH_TIMEOUT", "Batch execution deadline exceeded");
            } else {
                try {
                    String url = item.url();
                    if (item.fileId() != null) { url = fileStore.resolve(item.fileId()).toUri().toString(); }
                    DetectCommand command = new DetectCommand(batch.taskId(), batch.clientRequestId(), batch.traceId(),
                            batch.appId(), batch.sceneCode(), batch.contentType(), item.text(),
                            batch.contentType() == org.example.study.domain.ContentType.IMAGE ? url : null,
                            batch.contentType() == org.example.study.domain.ContentType.AUDIO ? url : null,
                            batch.contentType() == org.example.study.domain.ContentType.VIDEO ? url : null,
                            batch.executionMode(), batch.submittedAt());
                    DetectContext context = new DetectContext(command);
                    context.putAttribute("itemId", item.itemId());
                    context.putAttribute("language", batch.language() == null ? "" : batch.language());
                    context.putAttribute("region", batch.region() == null ? "" : batch.region());
                    var logContext = context.logContext();
                    context.setLogContext(new org.example.study.baseSdk.chain.ChainLogContext(logContext.logSpace(),
                            logContext.traceId(), logContext.taskId(), logContext.appId(), logContext.sceneCode(),
                            logContext.chainName(), logContext.chainVersion(), java.util.Map.of("itemId", item.itemId())));
                    var chainResult = java.util.Objects.requireNonNull(router.execute(batch.contentType(), context), "Chain result must not be null");
                    result = chainResult.success() ? resultMapper.from(chainResult)
                            : DetectResult.degradedPass(batch.taskId(), "DETECT_CHAIN_FAILED", "Detection chain execution failed");
                } catch (Exception exception) {
                    LOGGER.error("Batch item detection failed, taskId={}, itemId={}", batch.taskId(), item.itemId(), exception);
                    result = DetectResult.degradedPass(batch.taskId(), "DETECT_EXECUTION_FAILED", "Detection execution failed");
                }
            }
            results.add(DetectItemResult.from(item.itemId(), result));
        }
        return new BatchDetectResult(batch.taskId(), batch.contentType(), "COMPLETED", results);
    }

    public AsyncDetectAcceptedResult submitBatch(BatchDetectCommand command) {
        try (var lease = lifecycle.acquire()) {
            DetectTaskMessage message = new DetectTaskMessage("evt_" + compactUuid(), command.taskId(),
                    command.clientRequestId(), command.traceId(), command.appId(), command.sceneCode(),
                    command.contentType(), null, null, null, null, command.submittedAt(), command);
            try {
                if (taskQueue.publish(message)) {
                    return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(), DetectStatus.ACCEPTED,
                            null, command.submittedAt(), true, false, "", "");
                }
            } catch (RuntimeException exception) {
                LOGGER.error("Batch task publish failed, taskId={}", command.taskId(), exception);
            }
            return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(), DetectStatus.FAILED,
                    DetectAction.PASS, command.submittedAt(), false, true, "TASK_QUEUE_UNAVAILABLE", "Detection task was not queued");
        }
    }

    public DetectResult detect(DetectCommand command) {
        try (var lease = lifecycle.acquire()) {
            return detectAccepted(command);
        }
    }

    /** 已被异步处理器接收的任务继续完成，不在链内部再次检查准入。 */
    DetectResult detectAccepted(DetectCommand command) {
        try {
            return resultMapper.from(router.execute(command.contentType(), new DetectContext(command)));
        } catch (RuntimeException exception) {
            LOGGER.error("Synchronous detection degraded, taskId={}, contentType={}",
                    command.taskId(), command.contentType(), exception);
            return DetectResult.degradedPass(command.taskId(), "DETECT_EXECUTION_FAILED", "Detection execution failed");
        }
    }

    public AsyncDetectAcceptedResult submit(DetectCommand command) {
        try (var lease = lifecycle.acquire()) {
            return submitAccepted(command);
        }
    }

    private AsyncDetectAcceptedResult submitAccepted(DetectCommand command) {
        DetectTaskMessage message = new DetectTaskMessage(
                "evt_" + compactUuid(), command.taskId(), command.clientRequestId(), command.traceId(),
                command.appId(), command.sceneCode(), command.contentType(), command.text(), command.imageUrl(),
                command.audioUrl(), command.videoUrl(), command.submittedAt());
        try {
            if (taskQueue.publish(message)) {
                return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(),
                        DetectStatus.ACCEPTED, null, command.submittedAt(), true, false, "", "");
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Asynchronous task publish failed, taskId={}, contentType={}",
                    command.taskId(), command.contentType(), exception);
        }
        return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(),
                DetectStatus.FAILED, DetectAction.PASS,
                command.submittedAt(), false, true,
                "TASK_QUEUE_UNAVAILABLE", "Detection task was not queued");
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
