package org.example.study.application;

import org.example.study.baseSdk.chain.ChainRuntime;
import org.example.study.baseSdk.log.BaseLogEvent;
import org.example.study.baseSdk.log.LogLevel;
import org.example.study.baseSdk.log.LogRecorder;
import org.example.study.baseSdk.log.LogType;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectContext;
import org.example.study.domain.DetectRequest;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectResultMessage;
import org.example.study.domain.DetectStatus;
import org.example.study.domain.DetectTaskMessage;
import org.example.study.messaging.DetectResultPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 消费异步任务并发布终态结果，任务异常按故障开放策略返回 PASS。
 */
@Service
public class AsyncDetectTaskProcessor {

    private final ChainRuntime chainRuntime;
    private final DetectResultPublisher resultPublisher;
    private final TaskIdGenerator taskIdGenerator;
    private final LogRecorder logRecorder;

    public AsyncDetectTaskProcessor(
            ChainRuntime chainRuntime,
            DetectResultPublisher resultPublisher,
            TaskIdGenerator taskIdGenerator,
            LogRecorder logRecorder
    ) {
        this.chainRuntime = chainRuntime;
        this.resultPublisher = resultPublisher;
        this.taskIdGenerator = taskIdGenerator;
        this.logRecorder = logRecorder;
    }

    public void process(DetectTaskMessage taskMessage) {
        DetectResultMessage resultMessage;
        try {
            DetectRequest request = toRequest(taskMessage);
            DetectContext context = new DetectContext(
                    taskMessage.traceId(), taskMessage.taskId(), taskMessage.appId(), request
            );
            DetectResult result = DetectResult.success(
                    chainRuntime.execute(taskMessage.contentType().chainName(), context)
            );
            resultMessage = toSuccessMessage(taskMessage, result);
        } catch (RuntimeException exception) {
            recordFailure(taskMessage, exception);
            resultMessage = toDegradedMessage(taskMessage);
        }
        resultPublisher.publish(resultMessage);
    }

    private DetectRequest toRequest(DetectTaskMessage taskMessage) {
        return new DetectRequest(
                taskMessage.clientRequestId(), taskMessage.sceneCode(), taskMessage.contentType().name(),
                taskMessage.text(), taskMessage.imageUrl(), taskMessage.audioUrl(), taskMessage.videoUrl()
        );
    }

    private DetectResultMessage toSuccessMessage(DetectTaskMessage taskMessage, DetectResult result) {
        return new DetectResultMessage(
                taskIdGenerator.nextEventId(), "CONTENT_DETECT_COMPLETED", "1.0", Instant.now(),
                taskMessage.appId(), taskMessage.taskId(), taskMessage.clientRequestId(),
                taskMessage.contentType(), DetectStatus.SUCCEEDED, result.action(), result.labels(),
                false, "", ""
        );
    }

    private DetectResultMessage toDegradedMessage(DetectTaskMessage taskMessage) {
        return new DetectResultMessage(
                taskIdGenerator.nextEventId(), "CONTENT_DETECT_FAILED", "1.0", Instant.now(),
                taskMessage.appId(), taskMessage.taskId(), taskMessage.clientRequestId(),
                taskMessage.contentType(), DetectStatus.FAILED, DetectAction.PASS, List.of(), true,
                "DETECT_EXECUTION_FAILED", "Detection execution failed"
        );
    }

    private void recordFailure(DetectTaskMessage taskMessage, RuntimeException exception) {
        logRecorder.record(new BaseLogEvent(
                "content-risk", LogType.BIZ, LogLevel.ERROR, taskMessage.traceId(), taskMessage.taskId(),
                taskMessage.appId(), taskMessage.sceneCode(), "async-task-processor", "ASYNC_DETECT_DEGRADED",
                "Async detection execution failed", false, 0,
                Map.of(
                        "exceptionType", exception.getClass().getName(),
                        "exceptionMessage", exception.getMessage() == null ? "" : exception.getMessage()
                ),
                Instant.now()
        ));
    }
}
