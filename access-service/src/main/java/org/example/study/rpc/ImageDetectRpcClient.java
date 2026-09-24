package org.example.study.rpc;

import org.apache.dubbo.config.annotation.DubboReference;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectResult;
import org.springframework.stereotype.Component;

/** 图片检测服务客户端。 */
@Component
public class ImageDetectRpcClient {
    @DubboReference(url = "${IMAGE_DETECT_RPC_URL:tri://127.0.0.1:50052}", check = false,
            init = false, lazy = true, timeout = 5000, retries = 0)
    private ImageDetectRpcService remote;

    public DetectResult detect(DetectCommand command) {
        return remote.detect(command);
    }

    public AsyncDetectAcceptedResult submit(DetectCommand command) {
        return remote.submit(command);
    }
}
