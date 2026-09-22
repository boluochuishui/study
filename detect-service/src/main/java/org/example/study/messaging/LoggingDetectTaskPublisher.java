package org.example.study.messaging;

import org.example.study.baseSdk.log.BaseLogEvent;
import org.example.study.baseSdk.log.LogLevel;
import org.example.study.baseSdk.log.LogRecorder;
import org.example.study.baseSdk.log.LogType;
import org.example.study.domain.DetectTaskMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * 本地开发发布器，只记录任务，不提供跨进程可靠异步能力。
 */
@Component
@ConditionalOnProperty(name = "study.detect.async.publisher", havingValue = "logging", matchIfMissing = true)
public class LoggingDetectTaskPublisher implements DetectTaskPublisher {

    private final LogRecorder logRecorder;

    public LoggingDetectTaskPublisher(LogRecorder logRecorder) {
        this.logRecorder = logRecorder;
    }

    @Override
    public DetectTaskPublishReceipt publish(DetectTaskMessage message) {
        logRecorder.record(new BaseLogEvent(
                "content-risk", LogType.BIZ, LogLevel.INFO, message.traceId(), message.taskId(),
                message.appId(), message.sceneCode(), "logging-task-publisher", "ASYNC_TASK_PUBLISHED",
                "Async detection task published to local logging adapter", true, 0,
                Map.of("eventId", message.eventId(), "contentType", message.contentType().name()), Instant.now()
        ));
        return new DetectTaskPublishReceipt("local_" + message.eventId());
    }
}
