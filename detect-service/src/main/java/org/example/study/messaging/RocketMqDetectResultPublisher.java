package org.example.study.messaging;

import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.example.study.domain.DetectResultMessage;
import org.example.study.exception.DetectTaskPublishException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 将终态结果同步发送到租户独立 RocketMQ Topic。
 */
@Component
@ConditionalOnProperty(name = "study.detect.async.publisher", havingValue = "rocketmq")
public class RocketMqDetectResultPublisher implements DetectResultPublisher {

    private static final String RESULT_TAG = "DETECT_RESULT";
    private final ObjectMapper objectMapper;
    private final TenantResultTopicResolver topicResolver;
    private final ClientServiceProvider provider;
    private final Producer producer;

    public RocketMqDetectResultPublisher(
            ObjectMapper objectMapper,
            TenantResultTopicResolver topicResolver,
            @Value("${study.detect.async.rocketmq.endpoints}") String endpoints,
            @Value("${study.detect.async.rocketmq.access-key}") String accessKey,
            @Value("${study.detect.async.rocketmq.secret-key}") String secretKey
    ) {
        this.objectMapper = objectMapper;
        this.topicResolver = topicResolver;
        this.provider = ClientServiceProvider.loadService();
        try {
            ClientConfiguration configuration = ClientConfiguration.newBuilder()
                    .setEndpoints(endpoints)
                    .setCredentialProvider(new StaticSessionCredentialsProvider(accessKey, secretKey))
                    .build();
            this.producer = provider.newProducerBuilder()
                    .setClientConfiguration(configuration)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize RocketMQ result producer", exception);
        }
    }

    @Override
    public DetectTaskPublishReceipt publish(DetectResultMessage resultMessage) {
        try {
            String topic = topicResolver.resolve(resultMessage.appId());
            Message message = provider.newMessageBuilder()
                    .setTopic(topic)
                    .setTag(RESULT_TAG)
                    .setKeys(resultMessage.taskId())
                    .setBody(objectMapper.writeValueAsBytes(resultMessage))
                    .build();
            SendReceipt receipt = producer.send(message);
            return new DetectTaskPublishReceipt(receipt.getMessageId().toString());
        } catch (Exception exception) {
            throw new DetectTaskPublishException("Failed to publish detection result", exception);
        }
    }

    @PreDestroy
    public void close() {
        try {
            producer.close();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to close RocketMQ result producer", exception);
        }
    }
}
