package org.example.study.baseSdk.log;

import org.apache.logging.log4j.util.Strings;

import java.time.Instant;
import java.util.Map;

/**
 * 业务日志事件，用于保留业务层的日志门面。
 */
public record BizLogEvent(
        String logSpace,
        String traceId,
        String taskId,
        String appId,
        String sceneCode,
        String source,
        String eventType,
        String message,
        boolean success,
        long costMillis,
        Map<String, Object> attributes,
        Instant occurredAt
) {

    public BizLogEvent {
        logSpace = logSpace == null || logSpace.isBlank() ? "biz" : logSpace;
        source = source == null ? Strings.EMPTY : source;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    public BaseLogEvent toBaseLogEvent() {
        return new BaseLogEvent(
                logSpace,
                LogType.BIZ,
                LogLevel.INFO,
                traceId,
                taskId,
                appId,
                sceneCode,
                source,
                eventType,
                message,
                success,
                costMillis,
                attributes,
                occurredAt
        );
    }
}
