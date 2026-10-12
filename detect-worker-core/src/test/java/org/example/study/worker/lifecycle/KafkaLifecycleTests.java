package org.example.study.worker.lifecycle;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证消费者初始化期间不消费，摘流只暂停，在途收尾后才停止。 */
class KafkaLifecycleTests {
    @Test
    void startsOnlyWhenReadyAndStopsAfterDrain() {
        var registry = mock(KafkaListenerEndpointRegistry.class);
        var container = mock(MessageListenerContainer.class);
        var properties = new ContainerProperties("tasks");
        when(registry.getListenerContainer("modality-detect-tasks")).thenReturn(container);
        when(container.getContainerProperties()).thenReturn(properties);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(container).stop(any(Runnable.class));
        var participant = new KafkaLifecycleParticipant(registry);

        participant.prepare();
        verify(container, never()).start();
        assertThat(properties.isPauseImmediate()).isTrue();
        assertThat(properties.isStopImmediate()).isTrue();
        participant.onReady();
        participant.onDrain();
        verify(container, never()).stop(any(Runnable.class));
        participant.afterDrain(Duration.ofSeconds(1));

        var sequence = inOrder(container);
        sequence.verify(container).start();
        sequence.verify(container).pause();
        sequence.verify(container).stop(any(Runnable.class));
    }
}
