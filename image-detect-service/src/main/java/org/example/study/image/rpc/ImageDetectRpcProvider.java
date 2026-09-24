package org.example.study.image.rpc;

import org.apache.dubbo.config.annotation.DubboService;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectResult;
import org.example.study.rpc.ImageDetectRpcService;
import org.example.study.worker.ModalityWorkerService;

/** 图片检测 RPC 提供者。 */
@DubboService
public class ImageDetectRpcProvider implements ImageDetectRpcService {
    private final ModalityWorkerService workerService;

    public ImageDetectRpcProvider(ModalityWorkerService workerService) {
        this.workerService = workerService;
    }

    @Override
    public DetectResult detect(DetectCommand command) {
        requireType(command);
        return workerService.detect(command);
    }

    @Override
    public AsyncDetectAcceptedResult submit(DetectCommand command) {
        requireType(command);
        return workerService.submit(command);
    }

    private void requireType(DetectCommand command) {
        if (command == null || command.contentType() != ContentType.IMAGE) {
            throw new IllegalArgumentException("Detection command content type mismatch");
        }
    }
}
