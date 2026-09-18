package org.example.study.application;

import org.example.study.baseSdk.chain.ChainRuntime;
import org.example.study.baseSdk.log.BaseLogEvent;
import org.example.study.baseSdk.log.LogLevel;
import org.example.study.baseSdk.log.LogRecorder;
import org.example.study.baseSdk.log.LogType;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectContext;
import org.example.study.domain.DetectExecutionMode;
import org.example.study.domain.DetectRequest;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectStatus;
import org.example.study.domain.DetectTaskMessage;
import org.example.study.messaging.DetectTaskPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * 检测应用服务，分别承载同步执行和异步任务受理语义。
 */
@Service
public class DetectService {

    private static final String LOG_SPACE = "content-risk";
    private final ChainRuntime chainRuntime;
    private final DetectRequestValidator requestValidator;
    private final TaskIdGenerator taskIdGenerator;
    private final DetectTaskPublisher taskPublisher;
    private final LogRecorder logRecorder;

    public DetectService(
            ChainRuntime chainRuntime,
            DetectRequestValidator requestValidator,
            TaskIdGenerator taskIdGenerator,
            DetectTaskPublisher taskPublisher,
            LogRecorder logRecorder
    ) {
        this.chainRuntime = chainRuntime;
        this.requestValidator = requestValidator;
        this.taskIdGenerator = taskIdGenerator;
        this.taskPublisher = taskPublisher;
        this.logRecorder = logRecorder;
    }

    public DetectResult syncDetect(String appId, DetectRequest request) {
        ContentType contentType = requestValidator.validate(request, DetectExecutionMode.SYNC);
        String taskId = taskIdGenerator.nextTaskId();
        String traceId = taskIdGenerator.nextTraceId();
        DetectContext context = new DetectContext(traceId, taskId, appId, request);
        try {
            return DetectResult.success(chainRuntime.execute(contentType.chainName(), context));
        } catch (RuntimeException exception) {
            recordFailure(traceId, taskId, appId, request.sceneCode(), "SYNC_DETECT_DEGRADED", exception);
            return DetectResult.degradedPass(taskId, "DETECT_EXECUTION_FAILED", "Detection execution failed");
        }
    }

    public AsyncDetectAcceptedResult asyncDetect(String appId, DetectRequest request) {
        ContentType contentType = requestValidator.validate(request, DetectExecutionMode.ASYNC);
        String taskId = taskIdGenerator.nextTaskId();
        String traceId = taskIdGenerator.nextTraceId();
        Instant acceptedAt = Instant.now();
        DetectTaskMessage message = new DetectTaskMessage(
                taskIdGenerator.nextEventId(), taskId, request.clientRequestId(), traceId, appId,
                request.sceneCode(), contentType, request.text(), request.imageUrl(),
                request.audioUrl(), request.videoUrl(), acceptedAt
        );
        taskPublisher.publish(message);
        logRecorder.record(new BaseLogEvent(
                LOG_SPACE, LogType.API, LogLevel.INFO, traceId, taskId, appId, request.sceneCode(),
                "async-detect-api", "ASYNC_TASK_ACCEPTED", "Async detection task accepted", true, 0,
                Map.of("contentType", contentType.name()), acceptedAt
        ));
        return new AsyncDetectAcceptedResult(taskId, request.clientRequestId(), DetectStatus.ACCEPTED, acceptedAt);
    }

    private void recordFailure(
            String traceId,
            String taskId,
            String appId,
            String sceneCode,
            String eventType,
            RuntimeException exception
    ) {
        logRecorder.record(new BaseLogEvent(
                LOG_SPACE, LogType.API, LogLevel.ERROR, traceId, taskId, appId, sceneCode,
                "detect-service", eventType, "Detection execution failed", false, 0,
                Map.of(
                        "exceptionType", exception.getClass().getName(),
                        "exceptionMessage", exception.getMessage() == null ? "" : exception.getMessage()
                ),
                Instant.now()
        ));
    }
}
