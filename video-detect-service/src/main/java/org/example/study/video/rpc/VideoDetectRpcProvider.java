package org.example.study.video.rpc;

import org.apache.dubbo.config.annotation.DubboService;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectCommand;
import org.example.study.rpc.VideoDetectRpcService;
import org.example.study.worker.ModalityWorkerService;

/** 视频检测 RPC 提供者。 */
@DubboService
public class VideoDetectRpcProvider implements VideoDetectRpcService {
    private final ModalityWorkerService workerService;

    public VideoDetectRpcProvider(ModalityWorkerService workerService) {
        this.workerService = workerService;
    }

    @Override
    public AsyncDetectAcceptedResult submit(DetectCommand command) {
        requireType(command);
        return workerService.submit(command);
    }

    private void requireType(DetectCommand command) {
        if (command == null || command.contentType() != ContentType.VIDEO) {
            throw new IllegalArgumentException("Detection command content type mismatch");
        }
    }
}
