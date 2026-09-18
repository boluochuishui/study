package org.example.study.messaging;

import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.example.study.domain.DetectTaskMessage;
import org.example.study.exception.DetectTaskPublishException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 基于 RocketMQ 5.x gRPC 客户端的任务发布器。
 */
@Component
@ConditionalOnProperty(name = "study.detect.async.publisher", havingValue = "rocketmq")
public class RocketMqDetectTaskPublisher implements DetectTaskPublisher {

    private static final String TASK_TAG = "DETECT_TASK";
    private final ObjectMapper objectMapper;
    private final ClientServiceProvider provider;
    private final Producer producer;
    private final String topic;

    public RocketMqDetectTaskPublisher(
            ObjectMapper objectMapper,
            @Value("${study.detect.async.rocketmq.endpoints}") String endpoints,
            @Value("${study.detect.async.rocketmq.access-key}") String accessKey,
            @Value("${study.detect.async.rocketmq.secret-key}") String secretKey,
            @Value("${study.detect.async.rocketmq.task-topic:content-detect-task}") String topic
    ) {
        validateConfiguration(endpoints, accessKey, secretKey, topic);
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.provider = ClientServiceProvider.loadService();
        try {
            ClientConfiguration configuration = ClientConfiguration.newBuilder()
                    .setEndpoints(endpoints)
                    .setCredentialProvider(new StaticSessionCredentialsProvider(accessKey, secretKey))
                    .build();
            this.producer = provider.newProducerBuilder()
                    .setClientConfiguration(configuration)
                    .setTopics(topic)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize RocketMQ producer", exception);
        }
    }

    @Override
    public DetectTaskPublishReceipt publish(DetectTaskMessage taskMessage) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(taskMessage);
            Message message = provider.newMessageBuilder()
                    .setTopic(topic)
                    .setTag(TASK_TAG)
                    .setKeys(taskMessage.taskId())
                    .setBody(body)
                    .build();
            SendReceipt receipt = producer.send(message);
            return new DetectTaskPublishReceipt(receipt.getMessageId().toString());
        } catch (Exception exception) {
            throw new DetectTaskPublishException("Failed to publish detection task", exception);
        }
    }

    @PreDestroy
    public void close() {
        try {
            producer.close();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to close RocketMQ producer", exception);
        }
    }

    private void validateConfiguration(String endpoints, String accessKey, String secretKey, String topic) {
        if (isBlank(endpoints) || isBlank(accessKey) || isBlank(secretKey) || isBlank(topic)) {
            throw new IllegalArgumentException("RocketMQ publisher configuration is incomplete");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
