package org.example.study.messaging;

import org.example.study.domain.DetectResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 本地模式只记录结果事件。 */
@Component
@ConditionalOnProperty(name = "study.worker.messaging.mode", havingValue = "logging", matchIfMissing = true)
public class LoggingDetectResultQueue implements DetectResultQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingDetectResultQueue.class);

    @Override
    public void publish(DetectResultMessage message) {
        LOGGER.info("Detection result, taskId={}, status={}, action={}",
                message.taskId(), message.detectStatus(), message.action());
    }
}
