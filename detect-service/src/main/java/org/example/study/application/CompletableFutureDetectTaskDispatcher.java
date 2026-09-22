package org.example.study.application;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.example.study.baseSdk.log.BaseLogEvent;
import org.example.study.baseSdk.log.LogLevel;
import org.example.study.baseSdk.log.LogRecorder;
import org.example.study.baseSdk.log.LogType;
import org.example.study.domain.DetectTaskMessage;
import org.example.study.exception.DetectTaskPublishException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 使用受管线程池提交本地异步检测任务。
 */
@Component
public class CompletableFutureDetectTaskDispatcher {

    private final AsyncDetectTaskProcessor taskProcessor;
    private final Executor executor;
    private final LogRecorder logRecorder;

    public CompletableFutureDetectTaskDispatcher(
            AsyncDetectTaskProcessor taskProcessor,
            @Qualifier(DetectAsyncExecutorConfiguration.DETECT_ASYNC_EXECUTOR) Executor executor,
            LogRecorder logRecorder
    ) {
        this.taskProcessor = taskProcessor;
        this.executor = executor;
        this.logRecorder = logRecorder;
    }

    public CompletableFuture<Void> submit(DetectTaskMessage taskMessage) {
        try {
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> taskProcessor.process(taskMessage), executor);
            future.whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    recordFailure(taskMessage, "ASYNC_TASK_EXECUTION_FAILED", throwable);
                }
            });
            return future;
        } catch (RejectedExecutionException exception) {
            recordFailure(taskMessage, "ASYNC_TASK_REJECTED", exception);
            throw new DetectTaskPublishException("Failed to submit asynchronous detection task", exception);
        }
    }

    private void recordFailure(DetectTaskMessage taskMessage, String eventType, Throwable throwable) {
        logRecorder.record(new BaseLogEvent(
                "content-risk", LogType.BIZ, LogLevel.ERROR, taskMessage.traceId(), taskMessage.taskId(),
                taskMessage.appId(), taskMessage.sceneCode(), "completable-future-task-dispatcher", eventType,
                "Asynchronous detection task failed", false, 0,
                Map.of(
                        "exceptionType", throwable.getClass().getName(),
                        "exceptionMessage", throwable.getMessage() == null ? "" : throwable.getMessage(),
                        "contentType", taskMessage.contentType().name()
                ),
                Instant.now()
        ));
    }
}
