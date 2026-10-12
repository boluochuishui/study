package org.example.study.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.example.study.baseSdk.lifecycle.ServiceUnavailableException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Map;
import java.time.Duration;

/** Kafka 模式下启用监听器基础设施。 */
@EnableKafka
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "study.worker.messaging.mode", havingValue = "kafka")
public class KafkaWorkerConfiguration {
    /** 显式创建工厂，保证仅依赖 spring-kafka 时也能在 Kafka 模式启动。 */
    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, String> kafkaTemplate(@Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrap,
                                                       @Value("${study.worker.kafka.publish-timeout:3s}") Duration timeout) {
        Map<String, Object> properties = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.MAX_BLOCK_MS_CONFIG, timeout.toMillis());
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(properties));
    }

    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrap,
            @Value("${study.worker.kafka.consumer-group}") String group,
            @Value("${study.worker.kafka.max-poll-interval-ms:300000}") int maxPollInterval) {
        Map<String, Object> properties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap,
                ConsumerConfig.GROUP_ID_CONFIG, group,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollInterval,
                ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(properties));
        factory.setAutoStartup(false);
        factory.getContainerProperties().setPollTimeout(1000);
        factory.getContainerProperties().setPauseImmediate(true);
        factory.getContainerProperties().setStopImmediate(true);
        // 摘流窗口内尚未接收的记录保留偏移量，禁止按普通异常耗尽重试后跳过。
        DefaultErrorHandler errors = new DefaultErrorHandler();
        errors.setBackOffFunction((record, exception) -> isUnavailable(exception)
                ? new FixedBackOff(200, FixedBackOff.UNLIMITED_ATTEMPTS) : null);
        factory.setCommonErrorHandler(errors);
        return factory;
    }

    private boolean isUnavailable(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ServiceUnavailableException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
