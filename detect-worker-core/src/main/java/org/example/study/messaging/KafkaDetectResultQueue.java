package org.example.study.messaging;

import org.example.study.domain.DetectResultMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** 将检测终态结果发送到统一结果 Topic。 */
@Component
@ConditionalOnProperty(name = "study.worker.messaging.mode", havingValue = "kafka")
public class KafkaDetectResultQueue implements DetectResultQueue {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String resultTopic;
    private final Duration publishTimeout;

    public KafkaDetectResultQueue(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                                  @Value("${study.worker.kafka.result-topic:detect-result}") String resultTopic,
                                  @Value("${study.worker.kafka.publish-timeout:3s}") Duration publishTimeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.resultTopic = resultTopic;
        this.publishTimeout = publishTimeout;
    }

    @Override
    public void publish(DetectResultMessage message) {
        try {
            kafkaTemplate.send(resultTopic, message.taskId(), objectMapper.writeValueAsString(message))
                    .get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing detection result", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to publish detection result", exception);
        }
    }
}
