package org.example.study.rpc;

import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectResult;

/** 文本检测 RPC 契约。 */
public interface TextDetectRpcService {
    DetectResult detect(DetectCommand command);
    AsyncDetectAcceptedResult submit(DetectCommand command);
}
