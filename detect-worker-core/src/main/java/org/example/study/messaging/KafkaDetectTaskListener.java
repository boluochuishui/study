package org.example.study.messaging;

import org.example.study.domain.DetectTaskMessage;
import org.example.study.worker.AsyncDetectTaskProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 当前模态 Kafka 任务消费者。 */
@Component
@ConditionalOnProperty(name = "study.worker.messaging.mode", havingValue = "kafka")
public class KafkaDetectTaskListener {

    private final ObjectMapper objectMapper;
    private final AsyncDetectTaskProcessor processor;

    public KafkaDetectTaskListener(ObjectMapper objectMapper, AsyncDetectTaskProcessor processor) {
        this.objectMapper = objectMapper;
        this.processor = processor;
    }

    @KafkaListener(topics = "${study.worker.kafka.task-topic}", groupId = "${study.worker.kafka.consumer-group}")
    public void consume(String payload) {
        try {
            processor.process(objectMapper.readValue(payload, DetectTaskMessage.class));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse detection task", exception);
        }
    }
}
