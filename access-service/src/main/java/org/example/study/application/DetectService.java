package org.example.study.application;

import org.example.study.baseSdk.log.BaseLogEvent;
import org.example.study.baseSdk.log.LogLevel;
import org.example.study.baseSdk.log.LogRecorder;
import org.example.study.baseSdk.log.LogType;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectExecutionMode;
import org.example.study.domain.DetectRequest;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectStatus;
import org.example.study.rpc.AudioDetectRpcClient;
import org.example.study.rpc.ImageDetectRpcClient;
import org.example.study.rpc.TextDetectRpcClient;
import org.example.study.rpc.VideoDetectRpcClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * 接入层应用服务，仅负责校验、编号和 RPC 路由，不执行检测责任链。
 */
@Service
public class DetectService {

    private final DetectRequestValidator validator;
    private final TaskIdGenerator idGenerator;
    private final TextDetectRpcClient textClient;
    private final ImageDetectRpcClient imageClient;
    private final AudioDetectRpcClient audioClient;
    private final VideoDetectRpcClient videoClient;
    private final LogRecorder logRecorder;

    public DetectService(DetectRequestValidator validator, TaskIdGenerator idGenerator,
                         TextDetectRpcClient textClient, ImageDetectRpcClient imageClient,
                         AudioDetectRpcClient audioClient, VideoDetectRpcClient videoClient,
                         LogRecorder logRecorder) {
        this.validator = validator;
        this.idGenerator = idGenerator;
        this.textClient = textClient;
        this.imageClient = imageClient;
        this.audioClient = audioClient;
        this.videoClient = videoClient;
        this.logRecorder = logRecorder;
    }

    public DetectResult syncDetect(String appId, DetectRequest request) {
        DetectCommand command = command(appId, request, DetectExecutionMode.SYNC);
        ContentType type = command.contentType();
        try {
            return switch (type) {
                case TEXT -> textClient.detect(command);
                case IMAGE -> imageClient.detect(command);
                default -> throw new IllegalStateException("Unsupported synchronous content type");
            };
        } catch (RuntimeException exception) {
            recordFailure(command, "SYNC_RPC_DEGRADED", exception);
            return DetectResult.degraded(command, "DETECT_SERVICE_UNAVAILABLE",
                    "Detection service unavailable");
        }
    }

    public AsyncDetectAcceptedResult asyncDetect(String appId, DetectRequest request) {
        DetectCommand command = command(appId, request, DetectExecutionMode.ASYNC);
        ContentType type = command.contentType();
        try {
            return switch (type) {
                case TEXT -> textClient.submit(command);
                case IMAGE -> imageClient.submit(command);
                case AUDIO -> audioClient.submit(command);
                case VIDEO -> videoClient.submit(command);
                default -> throw new IllegalStateException("Unsupported asynchronous content type");
            };
        } catch (RuntimeException exception) {
            recordFailure(command, "ASYNC_RPC_DEGRADED", exception);
            return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(), DetectStatus.FAILED,
                    DetectAction.PASS, command.submittedAt(), false, true,
                    "DETECT_SERVICE_UNAVAILABLE",
                    "Detection service unavailable");
        }
    }

    private DetectCommand command(String appId, DetectRequest request,
                                  DetectExecutionMode executionMode) {
        var items = validator.prepare(request, executionMode);
        return new DetectCommand(idGenerator.nextTaskId(), request.getClientRequestId(), idGenerator.nextTraceId(),
                appId, request.getSceneCode(), ContentType.from(request.getContentType()), request.getLanguage(), request.getRegion(),
                executionMode, Instant.now(), items);
    }

    private void recordFailure(DetectCommand command, String eventType, RuntimeException exception) {
        logRecorder.log(new BaseLogEvent(
                "content-risk", LogType.API, LogLevel.ERROR, command.traceId(), command.taskId(), command.appId(),
                command.sceneCode(), "access-service", eventType, "Detection RPC failed", false, 0,
                Map.of("contentType", command.contentType().name(),
                        "exceptionType", exception.getClass().getName()), Instant.now()));
    }
}
