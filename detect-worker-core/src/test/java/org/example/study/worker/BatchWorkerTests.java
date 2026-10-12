package org.example.study.worker;

import java.time.Instant;
import java.util.List;
import org.example.study.baseSdk.lifecycle.LifecycleProperties;
import org.example.study.baseSdk.lifecycle.ServiceLifecycleManager;
import org.example.study.domain.BatchDetectCommand;
import org.example.study.domain.BatchDetectItem;
import org.example.study.domain.BatchDetectResult;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectExecutionMode;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectResultMessage;
import org.example.study.domain.DetectTaskMessage;
import org.example.study.messaging.DetectResultQueue;
import org.example.study.messaging.DetectTaskQueue;
import org.example.study.detect.common.ModalityDetectRouter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/** 验证一个父编号、一条任务消息和一条聚合结果消息。 */
class BatchWorkerTests {
    @Test
    void publishesOneTaskAndOneAggregateResult() {
        var lifecycle = lifecycle();
        var tasks = mock(DetectTaskQueue.class);
        when(tasks.publish(any())).thenReturn(true);
        var worker = new ModalityWorkerService(mock(ModalityDetectRouter.class), mock(DetectResultMapper.class), tasks, lifecycle);
        var batch = command();
        assertThat(worker.submitBatch(batch).resultExpected()).isTrue();
        var task = ArgumentCaptor.forClass(DetectTaskMessage.class);
        verify(tasks, times(1)).publish(task.capture());
        assertThat(task.getValue().batch().items()).hasSize(2);
        var results = mock(DetectResultQueue.class);
        new AsyncDetectTaskProcessor(worker, results, lifecycle).process(task.getValue());
        var message = ArgumentCaptor.forClass(DetectResultMessage.class);
        verify(results, times(1)).publish(message.capture());
        assertThat(message.getValue().taskId()).isEqualTo("task");
        assertThat(message.getValue().eventVersion()).isEqualTo("2.0");
        assertThat(message.getValue().results()).extracting(item -> item.itemId()).containsExactly("one", "two");
        assertThat(message.getValue().results()).allMatch(item -> item.degraded());
        assertThat(lifecycle.inFlight()).isZero();
    }

    @Test
    void itemFailureDoesNotStopFollowingItems() {
        var router = mock(ModalityDetectRouter.class);
        var mapper = mock(DetectResultMapper.class);
        var chainResult = new org.example.study.baseSdk.chain.ChainExecuteResult("trace", "task", "chain", true, List.of(), 1);
        when(router.execute(any(), any())).thenThrow(new IllegalStateException("Model failed")).thenReturn(chainResult);
        when(mapper.from(chainResult)).thenReturn(DetectResult.degradedPass("task", "TEST_SECOND", "Second item executed"));
        var worker = new ModalityWorkerService(router, mapper, mock(DetectTaskQueue.class), lifecycle());
        var original = command();
        var batch = new BatchDetectCommand(original.taskId(), original.clientRequestId(), original.traceId(), original.appId(),
                original.sceneCode(), ContentType.TEXT, "zh-CN", "CN", DetectExecutionMode.SYNC, original.submittedAt(), List.of(
                new BatchDetectItem("one", "first", null, null, "", ""), new BatchDetectItem("two", "second", null, null, "", "")));
        BatchDetectResult result = worker.detectBatch(batch);
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.results().getFirst().errorCode()).isEqualTo("DETECT_EXECUTION_FAILED");
        assertThat(result.results().getLast().errorCode()).isEqualTo("TEST_SECOND");
        verify(router, times(2)).execute(any(), any());
    }

    private BatchDetectCommand command() {
        return new BatchDetectCommand("task", "client", "trace", "app", "scene", ContentType.TEXT, null, null,
                DetectExecutionMode.ASYNC, Instant.now(), List.of(new BatchDetectItem("one", null, null, null, "INVALID_UPLOAD", "Invalid file"),
                new BatchDetectItem("two", null, null, null, "INVALID_UPLOAD", "Invalid file")));
    }

    @Test
    void chainFailureIsNotReportedAsSuccessfulDetection() {
        var router = mock(ModalityDetectRouter.class);
        when(router.execute(any(), any())).thenReturn(new org.example.study.baseSdk.chain.ChainExecuteResult(
                "trace", "task", "chain", false, List.of(), 1));
        var worker = new ModalityWorkerService(router, new DetectResultMapper(), mock(DetectTaskQueue.class), lifecycle());
        var original = command();
        var batch = new BatchDetectCommand(original.taskId(), original.clientRequestId(), original.traceId(), original.appId(),
                original.sceneCode(), ContentType.TEXT, null, null, DetectExecutionMode.SYNC, original.submittedAt(),
                List.of(new BatchDetectItem("one", "text", null, null, "", "")));
        assertThat(worker.detectBatch(batch).results().getFirst().errorCode()).isEqualTo("DETECT_CHAIN_FAILED");
    }

    private ServiceLifecycleManager lifecycle() {
        var manager = new ServiceLifecycleManager(List.of(), new LifecycleProperties());
        manager.ready();
        return manager;
    }
}
