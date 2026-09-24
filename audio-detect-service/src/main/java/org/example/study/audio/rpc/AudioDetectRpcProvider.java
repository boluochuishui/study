package org.example.study.audio.rpc;

import org.apache.dubbo.config.annotation.DubboService;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.ContentType;
import org.example.study.domain.DetectCommand;
import org.example.study.rpc.AudioDetectRpcService;
import org.example.study.worker.ModalityWorkerService;

/** 音频检测 RPC 提供者。 */
@DubboService
public class AudioDetectRpcProvider implements AudioDetectRpcService {
    private final ModalityWorkerService workerService;

    public AudioDetectRpcProvider(ModalityWorkerService workerService) {
        this.workerService = workerService;
    }

    @Override
    public AsyncDetectAcceptedResult submit(DetectCommand command) {
        requireType(command);
        return workerService.submit(command);
    }

    private void requireType(DetectCommand command) {
        if (command == null || command.contentType() != ContentType.AUDIO) {
            throw new IllegalArgumentException("Detection command content type mismatch");
        }
    }
}
