package org.example.study.worker;

import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectResultMessage;
import org.example.study.domain.DetectTaskMessage;
import org.example.study.messaging.DetectResultQueue;
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

    public AsyncDetectTaskProcessor(ModalityWorkerService workerService, DetectResultQueue resultQueue) {
        this.workerService = workerService;
        this.resultQueue = resultQueue;
    }

    public void process(DetectTaskMessage task) {
        DetectResult result = workerService.detect(toCommand(task));
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
                task.videoUrl(), task.submittedAt());
    }
}
