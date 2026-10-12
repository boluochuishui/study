package org.example.study.worker;

import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectExecutionMode;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectResultMessage;
import org.example.study.domain.DetectTaskMessage;
import org.example.study.messaging.DetectResultQueue;
import org.example.study.baseSdk.lifecycle.ServiceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/** 消费当前模态任务并发布统一结果事件。 */
@Service
public class AsyncDetectTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncDetectTaskProcessor.class);
    private final ModalityWorkerService workerService;
    private final DetectResultQueue resultQueue;
    private final ServiceLifecycleManager lifecycle;

    public AsyncDetectTaskProcessor(ModalityWorkerService workerService, DetectResultQueue resultQueue,
                                    ServiceLifecycleManager lifecycle) {
        this.workerService = workerService;
        this.resultQueue = resultQueue;
        this.lifecycle = lifecycle;
    }

    public void process(DetectTaskMessage task) {
        try (var lease = lifecycle.acquire()) {
            processAccepted(task);
        }
    }

    private void processAccepted(DetectTaskMessage task) {
        if (task.batch() != null) {
            processBatch(task);
            return;
        }
        DetectResult result = workerService.detectAccepted(toCommand(task));
        DetectResultMessage message = new DetectResultMessage(
                "evt_" + UUID.randomUUID().toString().replace("-", ""),
                result.degraded() ? "CONTENT_DETECT_FAILED" : "CONTENT_DETECT_COMPLETED", "1.0", Instant.now(),
                task.appId(), task.taskId(), task.clientRequestId(), task.contentType(), result.detectStatus(),
                result.action(), result.labels(), result.degraded(), result.errorCode(), result.errorMessage());
        try {
            resultQueue.publish(message);
        } catch (RuntimeException exception) {
            LOGGER.error("Detection result publish failed, taskId={}", task.taskId(), exception);
        }
    }

    private DetectCommand toCommand(DetectTaskMessage task) {
        return new DetectCommand(task.taskId(), task.clientRequestId(), task.traceId(), task.appId(),
                task.sceneCode(), task.contentType(), task.text(), task.imageUrl(), task.audioUrl(),
                task.videoUrl(), DetectExecutionMode.ASYNC, task.submittedAt());
    }

    private void processBatch(DetectTaskMessage task) {
        var batch = task.batch();
        if (!task.taskId().equals(batch.taskId()) || task.contentType() != batch.contentType()
                || !task.appId().equals(batch.appId()) || batch.executionMode() != DetectExecutionMode.ASYNC) {
            throw new IllegalArgumentException("Batch task envelope mismatch");
        }
        var result = workerService.detectBatchAccepted(batch);
        boolean degraded = result.results().stream().anyMatch(org.example.study.domain.DetectItemResult::degraded);
        var action = result.results().stream().map(org.example.study.domain.DetectItemResult::action)
                .max(java.util.Comparator.comparingInt(Enum::ordinal)).orElse(org.example.study.domain.DetectAction.PASS);
        var labels = result.results().stream().flatMap(item -> item.labels().stream()).distinct().toList();
        DetectResultMessage message = new DetectResultMessage("evt_" + UUID.randomUUID().toString().replace("-", ""),
                "CONTENT_DETECT_COMPLETED", "2.0", Instant.now(), task.appId(), task.taskId(), task.clientRequestId(),
                task.contentType(), degraded ? org.example.study.domain.DetectStatus.FAILED : org.example.study.domain.DetectStatus.SUCCEEDED,
                action, labels, degraded, degraded ? "BATCH_ITEM_FAILED" : "",
                degraded ? "One or more detection items failed" : "", result.results(), "COMPLETED");
        try { resultQueue.publish(message); }
        catch (RuntimeException exception) { LOGGER.error("Batch result publish failed, taskId={}", task.taskId(), exception); }
    }
}
