package org.example.study.worker;

import org.example.study.baseSdk.chain.ChainExecuteResult;
import org.example.study.detect.common.ModalityDetectRouter;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectStatus;
import org.example.study.messaging.DetectTaskQueue;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证模态 Worker 的同步降级和异步入队确认。 */
class ModalityWorkerServiceTests {

    private final ModalityDetectRouter router = mock(ModalityDetectRouter.class);
    private final DetectResultMapper mapper = mock(DetectResultMapper.class);
    private final DetectTaskQueue queue = mock(DetectTaskQueue.class);
    private final ModalityWorkerService service = new ModalityWorkerService(router, mapper, queue);

    @Test
    void returnsMappedSynchronousResult() {
        var chainResult = new ChainExecuteResult("trace", "task", "text.detect.chain", true, List.of(), 1);
        var expected = new org.example.study.domain.DetectResult(
                "task", DetectStatus.SUCCEEDED, DetectAction.PASS, List.of(), false, "", "");
        when(router.execute(any(), any())).thenReturn(chainResult);
        when(mapper.from(chainResult)).thenReturn(expected);

        assertThat(service.detect(command())).isSameAs(expected);
    }

    @Test
    void queueFailureDoesNotPromiseResult() {
        when(queue.publish(any())).thenReturn(false);

        var result = service.submit(command());

        assertThat(result.status()).isEqualTo(DetectStatus.FAILED);
        assertThat(result.degraded()).isTrue();
        assertThat(result.resultExpected()).isFalse();
        assertThat(result.errorCode()).isEqualTo("TASK_QUEUE_UNAVAILABLE");
    }

    @Test
    void queueSuccessReturnsAccepted() {
        when(queue.publish(any())).thenReturn(true);

        var result = service.submit(command());

        assertThat(result.status()).isEqualTo(DetectStatus.ACCEPTED);
        assertThat(result.resultExpected()).isTrue();
    }

    private DetectCommand command() {
        return new DetectCommand("task", "client", "trace", "app", "scene", ContentType.TEXT,
                "text", null, null, null, Instant.now());
    }
}
