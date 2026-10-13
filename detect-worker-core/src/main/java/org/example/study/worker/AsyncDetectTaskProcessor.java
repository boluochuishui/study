package org.example.study.worker;

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
        var result = workerService.detectAccepted(task.command());
        boolean degraded = result.getResults().stream().anyMatch(org.example.study.domain.DetectItemResult::isDegraded);
        var action = result.getResults().stream().map(org.example.study.domain.DetectItemResult::getAction)
                .max(java.util.Comparator.comparingInt(Enum::ordinal)).orElse(org.example.study.domain.DetectAction.PASS);
        var labels = result.getResults().stream().flatMap(item -> item.getLabels().stream()).distinct().toList();
        DetectResultMessage message = new DetectResultMessage("evt_" + UUID.randomUUID().toString().replace("-", ""),
                degraded ? "CONTENT_DETECT_FAILED" : "CONTENT_DETECT_COMPLETED", "2.0", Instant.now(), task.command().appId(), task.taskId(), task.command().clientRequestId(),
                task.contentType(), degraded ? org.example.study.domain.DetectStatus.FAILED : org.example.study.domain.DetectStatus.SUCCEEDED,
                action, labels, degraded, degraded ? "DETECT_ITEM_FAILED" : "",
                degraded ? "One or more detection items failed" : "", result.getResults(), "COMPLETED");
        try { resultQueue.publish(message); }
        catch (RuntimeException exception) { LOGGER.error("Detection result publish failed, taskId={}", task.taskId(), exception); }
    }
}
