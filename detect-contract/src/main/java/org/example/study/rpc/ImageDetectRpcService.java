package org.example.study.rpc;

import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectResult;
import org.example.study.domain.BatchDetectCommand;
import org.example.study.domain.BatchDetectResult;

/** 图片检测 RPC 契约。 */
public interface ImageDetectRpcService {
    BatchDetectResult detectBatch(BatchDetectCommand command);
    AsyncDetectAcceptedResult submitBatch(BatchDetectCommand command);
    DetectResult detect(DetectCommand command);
    AsyncDetectAcceptedResult submit(DetectCommand command);
}
