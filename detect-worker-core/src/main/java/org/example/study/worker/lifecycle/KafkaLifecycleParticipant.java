package org.example.study.worker.lifecycle;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.example.study.baseSdk.lifecycle.LifecycleParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

/** 检测消费者在就绪后启动，下线时暂停领取，已有任务完成后再停止容器。 */
@Component
@Order(0)
@ConditionalOnProperty(name = "study.worker.messaging.mode", havingValue = "kafka")
public class KafkaLifecycleParticipant implements LifecycleParticipant {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaLifecycleParticipant.class);
    private final KafkaListenerEndpointRegistry registry;

    public KafkaLifecycleParticipant(KafkaListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    private MessageListenerContainer container() {
        MessageListenerContainer container = registry.getListenerContainer("modality-detect-tasks");
        if (container == null) {
            throw new IllegalStateException("Detection Kafka listener container is missing");
        }
        return container;
    }

    @Override
    public void prepare() {
        container().getContainerProperties().setPauseImmediate(true);
        container().getContainerProperties().setStopImmediate(true);
    }

    @Override
    public void onReady() {
        container().start();
    }

    @Override
    public void onDrain() {
        container().pause();
    }

    @Override
    public void afterDrain(Duration remaining) {
        CountDownLatch stopped = new CountDownLatch(1);
        container().stop(stopped::countDown);
        try {
            if (!stopped.await(remaining.toNanos(), TimeUnit.NANOSECONDS)) {
                LOGGER.warn("KAFKA_STOP_TIMEOUT");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("KAFKA_STOP_INTERRUPTED", exception);
        }
    }
}
