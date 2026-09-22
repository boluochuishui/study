package org.example.study.messaging;

import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.example.study.application.AsyncDetectTaskProcessor;
import org.example.study.baseSdk.log.BaseLogEvent;
import org.example.study.baseSdk.log.LogLevel;
import org.example.study.baseSdk.log.LogRecorder;
import org.example.study.baseSdk.log.LogType;
import org.example.study.domain.DetectTaskMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;

/**
 * RocketMQ 异步检测任务消费者。
 */
@Component
@ConditionalOnProperty(name = "study.detect.async.publisher", havingValue = "rocketmq")
public class RocketMqDetectTaskConsumer {

    private static final String TASK_TAG = "DETECT_TASK";
    private final ObjectMapper objectMapper;
    private final AsyncDetectTaskProcessor taskProcessor;
    private final LogRecorder logRecorder;
    private final PushConsumer consumer;

    public RocketMqDetectTaskConsumer(
            ObjectMapper objectMapper,
            AsyncDetectTaskProcessor taskProcessor,
            LogRecorder logRecorder,
            @Value("${study.detect.async.rocketmq.endpoints}") String endpoints,
            @Value("${study.detect.async.rocketmq.access-key}") String accessKey,
            @Value("${study.detect.async.rocketmq.secret-key}") String secretKey,
            @Value("${study.detect.async.rocketmq.task-topic:content-detect-task}") String taskTopic,
            @Value("${study.detect.async.rocketmq.consumer-group:content-detect-worker}") String consumerGroup
    ) {
        this.objectMapper = objectMapper;
        this.taskProcessor = taskProcessor;
        this.logRecorder = logRecorder;
        try {
            ClientConfiguration configuration = ClientConfiguration.newBuilder()
                    .setEndpoints(endpoints)
                    .setCredentialProvider(new StaticSessionCredentialsProvider(accessKey, secretKey))
                    .build();
            FilterExpression expression = new FilterExpression(TASK_TAG, FilterExpressionType.TAG);
            this.consumer = ClientServiceProvider.loadService()
                    .newPushConsumerBuilder()
                    .setClientConfiguration(configuration)
                    .setConsumerGroup(consumerGroup)
                    .setSubscriptionExpressions(Map.of(taskTopic, expression))
                    .setMessageListener(messageView -> consume(messageView.getBody()))
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize RocketMQ task consumer", exception);
        }
    }

    private ConsumeResult consume(ByteBuffer body) {
        try {
            ByteBuffer copy = body.asReadOnlyBuffer();
            byte[] payload = new byte[copy.remaining()];
            copy.get(payload);
            DetectTaskMessage taskMessage = objectMapper.readValue(payload, DetectTaskMessage.class);
            taskProcessor.process(taskMessage);
            return ConsumeResult.SUCCESS;
        } catch (Exception exception) {
            logRecorder.record(new BaseLogEvent(
                    "content-risk", LogType.BIZ, LogLevel.ERROR, "", "", "", "",
                    "rocketmq-task-consumer", "ASYNC_TASK_CONSUME_FAILED", "Failed to consume async detection task",
                    false, 0,
                    Map.of(
                            "exceptionType", exception.getClass().getName(),
                            "exceptionMessage", exception.getMessage() == null ? "" : exception.getMessage()
                    ),
                    Instant.now()
            ));
            return ConsumeResult.FAILURE;
        }
    }

    @PreDestroy
    public void close() {
        try {
            consumer.close();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to close RocketMQ task consumer", exception);
        }
    }
}
