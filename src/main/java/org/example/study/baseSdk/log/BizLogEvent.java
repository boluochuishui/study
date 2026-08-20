package org.example.study.baseSdk.log;

import java.time.Instant;
import java.util.Map;

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

    public static BizLogEvent of(
            String eventType,
            String message,
            boolean success,
            long costMillis,
            Map<String, Object> attributes
    ) {
        LogContext context = LogContextHolder.current();
        return new BizLogEvent(
                context.traceId(),
                context.taskId(),
                context.appId(),
                context.sceneCode(),
                eventType,
                message,
                success,
                costMillis,
                attributes == null ? Map.of() : Map.copyOf(attributes),
                Instant.now()
        );
    }
}
