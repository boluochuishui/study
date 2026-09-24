package org.example.study.domain;

import java.time.Instant;
import java.io.Serializable;

/**
 * 异步任务成功写入消息队列后的受理结果。
 */
public record AsyncDetectAcceptedResult(
        String taskId,
        String clientRequestId,
        DetectStatus status,
        DetectAction action,
        Instant acceptedAt,
        boolean resultExpected,
        boolean degraded,
        String errorCode,
        String errorMessage
) implements Serializable {

    public AsyncDetectAcceptedResult(String taskId, String clientRequestId, DetectStatus status, Instant acceptedAt) {
        this(taskId, clientRequestId, status, null, acceptedAt, true, false, "", "");
    }

    public AsyncDetectAcceptedResult {
        errorCode = errorCode == null ? "" : errorCode;
        errorMessage = errorMessage == null ? "" : errorMessage;
    }
}
