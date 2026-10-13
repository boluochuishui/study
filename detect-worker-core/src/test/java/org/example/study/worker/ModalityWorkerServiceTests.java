package org.example.study.worker;

import org.example.study.baseSdk.chain.ChainExecuteResult;
import org.example.study.baseSdk.lifecycle.LifecycleProperties;
import org.example.study.baseSdk.lifecycle.ServiceLifecycleManager;
import org.example.study.baseSdk.lifecycle.ServiceUnavailableException;
import org.example.study.detect.common.ModalityDetectRouter;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectExecutionMode;
import org.example.study.domain.DetectStatus;
import org.example.study.messaging.DetectTaskQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证模态 Worker 的同步降级和异步入队确认。 */
class ModalityWorkerServiceTests {

    private final ModalityDetectRouter router = mock(ModalityDetectRouter.class);
    private final DetectResultMapper mapper = mock(DetectResultMapper.class);
    private final DetectTaskQueue queue = mock(DetectTaskQueue.class);
    private final ServiceLifecycleManager lifecycle = new ServiceLifecycleManager(List.of(), new LifecycleProperties());
    private final ModalityWorkerService service = new ModalityWorkerService(router, mapper, queue, lifecycle);

    @BeforeEach
    void ready() {
        lifecycle.ready();
    }

    @Test
    void drainingRejectsNewDetectionAndQueueSubmission() {
        lifecycle.beginDrain();
        assertThatThrownBy(() -> service.detect(command())).isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> service.submit(asyncCommand())).isInstanceOf(ServiceUnavailableException.class);
        assertThat(lifecycle.inFlight()).isZero();
    }

    @Test
    void returnsMappedSynchronousResult() {
        var chainResult = new ChainExecuteResult("trace", "task", "text.detect.chain", true, List.of(), 1);
        var expected = new org.example.study.domain.DetectItemResult(
                "one", DetectStatus.SUCCEEDED, DetectAction.PASS, List.of(), false, "", "");
        when(router.execute(any(), any())).thenReturn(chainResult);
        when(mapper.from("one", chainResult)).thenReturn(expected);

        assertThat(service.detect(command()).getResults()).containsExactly(expected);
    }

    @Test
    void queueFailureDoesNotPromiseResult() {
        when(queue.publish(any())).thenReturn(false);

        var result = service.submit(asyncCommand());

        assertThat(result.getStatus()).isEqualTo(DetectStatus.FAILED);
        assertThat(result.isDegraded()).isTrue();
        assertThat(result.isResultExpected()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("TASK_QUEUE_UNAVAILABLE");
    }

    @Test
    void queueSuccessReturnsAccepted() {
        when(queue.publish(any())).thenReturn(true);

        var result = service.submit(asyncCommand());

        assertThat(result.getStatus()).isEqualTo(DetectStatus.ACCEPTED);
        assertThat(result.isResultExpected()).isTrue();
    }

    private DetectCommand asyncCommand() {
        var command = command();
        return new DetectCommand(command.taskId(), command.clientRequestId(), command.traceId(), command.appId(),
                command.sceneCode(), command.contentType(), null, null, DetectExecutionMode.ASYNC,
                command.submittedAt(), command.items());
    }

    private DetectCommand command() {
        return new DetectCommand("task", "client", "trace", "app", "scene", ContentType.TEXT, null, null, DetectExecutionMode.SYNC, Instant.now(),
                java.util.List.of(new org.example.study.domain.DetectItem("one", "text", null, null, "", "")));
    }
}
