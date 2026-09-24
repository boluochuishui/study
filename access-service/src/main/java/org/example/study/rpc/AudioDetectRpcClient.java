package org.example.study.rpc;

import org.apache.dubbo.config.annotation.DubboReference;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectCommand;
import org.springframework.stereotype.Component;

/** 音频检测服务客户端。 */
@Component
public class AudioDetectRpcClient {
    @DubboReference(url = "${AUDIO_DETECT_RPC_URL:tri://127.0.0.1:50053}", check = false,
            init = false, lazy = true, timeout = 3000, retries = 0)
    private AudioDetectRpcService remote;

    public AsyncDetectAcceptedResult submit(DetectCommand command) {
        return remote.submit(command);
    }
}
