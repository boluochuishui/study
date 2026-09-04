package org.example.study.baseSdk.log;

import java.time.Instant;
import java.util.Map;

/**
 * Structured business log event emitted by platform components.
 */
public record BizLogEvent(
        String traceId,
        String taskId,
        String appId,
        String sceneCode,
        String eventType,
        String message,
        boolean success,
        long costMillis,
        Map<String, Object> attributes,
        Instant occurredAt
) {
}
