package org.example.study.worker;

import java.time.Instant;
import java.util.List;

import org.example.study.baseSdk.lifecycle.LifecycleProperties;
import org.example.study.baseSdk.lifecycle.ServiceLifecycleManager;
import org.example.study.baseSdk.lifecycle.ServiceUnavailableException;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectTaskMessage;
import org.example.study.messaging.DetectResultQueue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 异步任务的在途租约覆盖责任链和结果发布两个阶段。 */
class AsyncLifecycleTests {
    @Test
    void acceptedTaskCanPublishResultEvenWhenDrainingStartsDuringDetection() {
        var lifecycle = new ServiceLifecycleManager(List.of(), new LifecycleProperties());
        lifecycle.ready();
        var worker = mock(ModalityWorkerService.class);
        var queue = mock(DetectResultQueue.class);
        when(worker.detectAccepted(any())).thenAnswer(invocation -> {
            assertThat(lifecycle.inFlight()).isEqualTo(1);
            lifecycle.beginDrain();
            return DetectResult.degradedPass("task", "TEST", "Test result");
        });
        doAnswer(invocation -> {
            assertThat(lifecycle.inFlight()).isEqualTo(1);
            return null;
        }).when(queue).publish(any());
        new AsyncDetectTaskProcessor(worker, queue, lifecycle).process(task());
        verify(queue).publish(any());
        assertThat(lifecycle.inFlight()).isZero();
    }

    @Test
    void unacceptedKafkaTaskIsRejectedBeforeDetection() {
        var lifecycle = new ServiceLifecycleManager(List.of(), new LifecycleProperties());
        lifecycle.ready();
        lifecycle.beginDrain();
        var worker = mock(ModalityWorkerService.class);
        var queue = mock(DetectResultQueue.class);
        assertThatThrownBy(() -> new AsyncDetectTaskProcessor(worker, queue, lifecycle).process(task()))
                .isInstanceOf(ServiceUnavailableException.class);
        verify(worker, never()).detectAccepted(any());
        verify(queue, never()).publish(any());
    }

    private DetectTaskMessage task() {
        return new DetectTaskMessage("event", "task", "client", "trace", "app", "scene", ContentType.TEXT,
                "text", null, null, null, Instant.now());
    }
}
