package org.example.study.messaging;

import org.example.study.domain.DetectTaskMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 本地未启用 Kafka 时明确返回降级放通，不伪装成已入队。 */
@Component
@ConditionalOnProperty(name = "study.worker.messaging.mode", havingValue = "logging", matchIfMissing = true)
public class LoggingDetectTaskQueue implements DetectTaskQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingDetectTaskQueue.class);

    @Override
    public boolean publish(DetectTaskMessage message) {
        LOGGER.warn("Kafka task queue is disabled, taskId={}, contentType={}", message.taskId(), message.contentType());
        return false;
    }
}
