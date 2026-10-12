package org.example.study.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.BatchDetectCommand;
import org.example.study.domain.BatchDetectRequest;
import org.example.study.domain.BatchDetectResult;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectExecutionMode;
import org.example.study.domain.DetectStatus;
import org.example.study.rpc.TextDetectRpcClient;
import org.example.study.rpc.ImageDetectRpcClient;
import org.example.study.rpc.AudioDetectRpcClient;
import org.example.study.rpc.VideoDetectRpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 一次请求仅进行一次 RPC，内部逐项执行和汇总由模态 Worker 负责。 */
@Service
public class BatchDetectService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchDetectService.class);
    private final BatchRequestPreparer preparer;
    private final TaskIdGenerator ids;
    private final TextDetectRpcClient text;
    private final ImageDetectRpcClient image;
    private final AudioDetectRpcClient audio;
    private final VideoDetectRpcClient video;

    public BatchDetectService(BatchRequestPreparer preparer, TaskIdGenerator ids, TextDetectRpcClient text,
                              ImageDetectRpcClient image, AudioDetectRpcClient audio, VideoDetectRpcClient video) {
        this.preparer = preparer;
        this.ids = ids;
        this.text = text;
        this.image = image;
        this.audio = audio;
        this.video = video;
    }

    public BatchDetectResult sync(String appId, BatchDetectRequest request, Map<String, List<MultipartFile>> files) {
        var command = command(appId, request, files, DetectExecutionMode.SYNC);
        try {
            return switch (command.contentType()) {
                case TEXT -> text.detectBatch(command);
                case IMAGE -> image.detectBatch(command);
                default -> throw new IllegalArgumentException("Unsupported synchronous content type");
            };
        } catch (RuntimeException exception) {
            LOGGER.error("Batch synchronous RPC failed, taskId={}", command.taskId(), exception);
            return BatchDetectResult.degraded(command, "DETECT_SERVICE_UNAVAILABLE", "Detection service unavailable");
        }
    }

    public AsyncDetectAcceptedResult async(String appId, BatchDetectRequest request, Map<String, List<MultipartFile>> files) {
        var command = command(appId, request, files, DetectExecutionMode.ASYNC);
        try {
            return switch (command.contentType()) {
                case TEXT -> text.submitBatch(command);
                case IMAGE -> image.submitBatch(command);
                case AUDIO -> audio.submitBatch(command);
                case VIDEO -> video.submitBatch(command);
                default -> throw new IllegalArgumentException("Unsupported asynchronous content type");
            };
        } catch (RuntimeException exception) {
            LOGGER.error("Batch asynchronous RPC failed, taskId={}", command.taskId(), exception);
            return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(), DetectStatus.FAILED,
                    DetectAction.PASS, command.submittedAt(), false, true, "DETECT_SERVICE_UNAVAILABLE", "Detection service unavailable");
        }
    }

    private BatchDetectCommand command(String appId, BatchDetectRequest request, Map<String, List<MultipartFile>> files,
                                        DetectExecutionMode mode) {
        var items = preparer.prepare(request, mode, files);
        return new BatchDetectCommand(ids.nextTaskId(), request.clientRequestId(), ids.nextTraceId(), appId,
                request.sceneCode(), ContentType.from(request.contentType()), request.language(), request.region(), mode, Instant.now(), items);
    }
}
