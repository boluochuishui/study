package org.example.study.rpc;

import org.apache.dubbo.config.annotation.DubboReference;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectResult;
import org.springframework.stereotype.Component;

/** 文本检测服务客户端。 */
@Component
public class TextDetectRpcClient {
    @DubboReference(url = "${TEXT_DETECT_RPC_URL:tri://127.0.0.1:50051}", check = false,
            init = false, lazy = true, timeout = 3000, retries = 0)
    private TextDetectRpcService remote;

    public DetectResult detect(DetectCommand command) {
        return remote.detect(command);
    }

    public AsyncDetectAcceptedResult submit(DetectCommand command) {
        return remote.submit(command);
    }
}
