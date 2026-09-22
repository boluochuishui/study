package org.example.study.messaging;

import org.example.study.baseSdk.log.BaseLogEvent;
import org.example.study.baseSdk.log.LogLevel;
import org.example.study.baseSdk.log.LogRecorder;
import org.example.study.baseSdk.log.LogType;
import org.example.study.domain.DetectResultMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * 本地开发结果发布器，只输出结构化日志。
 */
@Component
@ConditionalOnProperty(name = "study.detect.async.publisher", havingValue = "logging", matchIfMissing = true)
public class LoggingDetectResultPublisher implements DetectResultPublisher {

    private final LogRecorder logRecorder;

    public LoggingDetectResultPublisher(LogRecorder logRecorder) {
        this.logRecorder = logRecorder;
    }

    @Override
    public DetectTaskPublishReceipt publish(DetectResultMessage message) {
        logRecorder.record(new BaseLogEvent(
                "content-risk", LogType.BIZ,
                message.degraded() ? LogLevel.ERROR : LogLevel.INFO,
                "", message.taskId(), message.appId(), "", "logging-result-publisher",
                "ASYNC_RESULT_PUBLISHED", "Async detection result published to local logging adapter",
                !message.degraded(), 0,
                Map.of(
                        "eventId", message.eventId(),
                        "detectStatus", message.detectStatus().name(),
                        "action", message.action().name()
                ),
                Instant.now()
        ));
        return new DetectTaskPublishReceipt("local_" + message.eventId());
    }
}
