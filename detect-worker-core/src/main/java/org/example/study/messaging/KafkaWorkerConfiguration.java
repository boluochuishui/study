package org.example.study.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/** Kafka 模式下启用监听器基础设施。 */
@EnableKafka
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "study.worker.messaging.mode", havingValue = "kafka")
public class KafkaWorkerConfiguration {
}
