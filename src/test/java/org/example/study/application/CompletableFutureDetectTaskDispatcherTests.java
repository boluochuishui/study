package org.example.study.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.example.study.domain.ContentType;
import org.example.study.domain.DetectTaskMessage;
import org.example.study.exception.DetectTaskPublishException;
import org.junit.jupiter.api.Test;

class CompletableFutureDetectTaskDispatcherTests {

    @Test
    void shouldProcessTaskWithConfiguredExecutor() {
        AsyncDetectTaskProcessor processor = mock(AsyncDetectTaskProcessor.class);
        Executor directExecutor = Runnable::run;
        CompletableFutureDetectTaskDispatcher dispatcher = new CompletableFutureDetectTaskDispatcher(
                processor, directExecutor, event -> { });
        DetectTaskMessage message = taskMessage();

        assertThat(dispatcher.submit(message)).isCompleted();
        verify(processor).process(message);
    }

    @Test
    void shouldConvertExecutorRejectionToPublishException() {
        Executor rejectingExecutor = command -> {
            throw new RejectedExecutionException("Queue is full");
        };
        CompletableFutureDetectTaskDispatcher dispatcher = new CompletableFutureDetectTaskDispatcher(
                mock(AsyncDetectTaskProcessor.class), rejectingExecutor, event -> { });

        assertThatThrownBy(() -> dispatcher.submit(taskMessage()))
                .isInstanceOf(DetectTaskPublishException.class)
                .hasMessage("Failed to submit asynchronous detection task");
    }

    private DetectTaskMessage taskMessage() {
        return new DetectTaskMessage(
                "event-1", "task-1", "request-1", "trace-1", "app-1", "comment",
                ContentType.TEXT, "normal", null, null, null, Instant.now());
    }
}
