package org.example.study.domain;

import java.time.Instant;
import java.util.List;

/**
 * 发送给外部租户的异步检测结果消息。
 */
public record DetectResultMessage(
        String eventId,
        String eventType,
        String eventVersion,
        Instant occurredAt,
        String appId,
        String taskId,
        String clientRequestId,
        ContentType contentType,
        DetectStatus detectStatus,
        DetectAction action,
        List<String> labels,
        boolean degraded,
        String errorCode,
        String errorMessage,
        List<DetectItemResult> results,
        String status
) {

    public DetectResultMessage {
        labels = labels == null ? List.of() : List.copyOf(labels);
        errorCode = errorCode == null ? "" : errorCode;
        errorMessage = errorMessage == null ? "" : errorMessage;
        results = results == null ? List.of() : List.copyOf(results);
    }

    public DetectResultMessage(String eventId, String eventType, String eventVersion, Instant occurredAt,
                               String appId, String taskId, String clientRequestId, ContentType contentType,
                               DetectStatus detectStatus, DetectAction action, List<String> labels,
                               boolean degraded, String errorCode, String errorMessage) {
        this(eventId, eventType, eventVersion, occurredAt, appId, taskId, clientRequestId, contentType,
                detectStatus, action, labels, degraded, errorCode, errorMessage, List.of(), null);
    }
}
