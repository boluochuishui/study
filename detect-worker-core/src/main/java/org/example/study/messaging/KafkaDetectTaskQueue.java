package org.example.study.messaging;

import org.example.study.domain.DetectTaskMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 将任务写入当前模态自己的 Kafka Topic，成功确认后才返回已受理。
 */
@Component
@ConditionalOnProperty(name = "study.worker.messaging.mode", havingValue = "kafka")
public class KafkaDetectTaskQueue implements DetectTaskQueue {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String taskTopic;
    private final Duration publishTimeout;

    public KafkaDetectTaskQueue(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                                @Value("${study.worker.kafka.task-topic}") String taskTopic,
                                @Value("${study.worker.kafka.publish-timeout:3s}") Duration publishTimeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.taskTopic = taskTopic;
        this.publishTimeout = publishTimeout;
    }

    @Override
    public boolean publish(DetectTaskMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(taskTopic, message.taskId(), payload)
                    .get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to publish detection task", exception);
        }
    }
}
