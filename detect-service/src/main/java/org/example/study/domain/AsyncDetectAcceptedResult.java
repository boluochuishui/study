package org.example.study.domain;

import java.time.Instant;

/**
 * 异步任务成功写入消息队列后的受理结果。
 */
public record AsyncDetectAcceptedResult(
        String taskId,
        String clientRequestId,
        DetectStatus status,
        Instant acceptedAt
) {
}
