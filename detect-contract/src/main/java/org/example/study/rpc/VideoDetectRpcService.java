package org.example.study.rpc;

import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectCommand;

/** 视频检测 RPC 契约，仅提供异步受理。 */
public interface VideoDetectRpcService {
    AsyncDetectAcceptedResult submit(DetectCommand command);
}
