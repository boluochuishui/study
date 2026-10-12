package org.example.study.worker;

import org.example.study.detect.common.ModalityDetectRouter;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectContext;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectStatus;
import org.example.study.domain.DetectTaskMessage;
import org.example.study.messaging.DetectTaskQueue;
import org.example.study.baseSdk.lifecycle.ServiceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 单模态服务的同步执行和异步入队能力。
 */
@Service
public class ModalityWorkerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModalityWorkerService.class);

    private final ModalityDetectRouter router;
    private final DetectResultMapper resultMapper;
    private final DetectTaskQueue taskQueue;
    private final ServiceLifecycleManager lifecycle;

    public ModalityWorkerService(ModalityDetectRouter router, DetectResultMapper resultMapper, DetectTaskQueue taskQueue,
                                 ServiceLifecycleManager lifecycle) {
        this.router = router;
        this.resultMapper = resultMapper;
        this.taskQueue = taskQueue;
        this.lifecycle = lifecycle;
    }

    public DetectResult detect(DetectCommand command) {
        try (var lease = lifecycle.acquire()) {
            return detectAccepted(command);
        }
    }

    /** 已被异步处理器接收的任务继续完成，不在链内部再次检查准入。 */
    DetectResult detectAccepted(DetectCommand command) {
        try {
            return resultMapper.from(router.execute(command.contentType(), new DetectContext(command)));
        } catch (RuntimeException exception) {
            LOGGER.error("Synchronous detection degraded, taskId={}, contentType={}",
                    command.taskId(), command.contentType(), exception);
            return DetectResult.degradedPass(command.taskId(), "DETECT_EXECUTION_FAILED", "Detection execution failed");
        }
    }

    public AsyncDetectAcceptedResult submit(DetectCommand command) {
        try (var lease = lifecycle.acquire()) {
            return submitAccepted(command);
        }
    }

    private AsyncDetectAcceptedResult submitAccepted(DetectCommand command) {
        DetectTaskMessage message = new DetectTaskMessage(
                "evt_" + compactUuid(), command.taskId(), command.clientRequestId(), command.traceId(),
                command.appId(), command.sceneCode(), command.contentType(), command.text(), command.imageUrl(),
                command.audioUrl(), command.videoUrl(), command.submittedAt());
        try {
            if (taskQueue.publish(message)) {
                return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(),
                        DetectStatus.ACCEPTED, null, command.submittedAt(), true, false, "", "");
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Asynchronous task publish failed, taskId={}, contentType={}",
                    command.taskId(), command.contentType(), exception);
        }
        return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(),
                DetectStatus.FAILED, DetectAction.PASS,
                command.submittedAt(), false, true,
                "TASK_QUEUE_UNAVAILABLE", "Detection task was not queued");
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
