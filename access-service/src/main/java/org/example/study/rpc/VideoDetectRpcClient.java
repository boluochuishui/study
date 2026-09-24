package org.example.study.rpc;

import org.apache.dubbo.config.annotation.DubboReference;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectCommand;
import org.springframework.stereotype.Component;

/** 视频检测服务客户端。 */
@Component
public class VideoDetectRpcClient {
    @DubboReference(url = "${VIDEO_DETECT_RPC_URL:tri://127.0.0.1:50054}", check = false,
            init = false, lazy = true, timeout = 3000, retries = 0)
    private VideoDetectRpcService remote;

    public AsyncDetectAcceptedResult submit(DetectCommand command) {
        return remote.submit(command);
    }
}
