package org.example.study.baseSdk.log;

import java.time.Instant;
import java.util.Map;

/**
 * baseSdk 通用结构化日志事件。
 */
public record BaseLogEvent(
        String logSpace,
        LogType logType,
        LogLevel logLevel,
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

    public BaseLogEvent {
        logSpace = normalize(logSpace, "default");
        logType = logType == null ? LogType.BIZ : logType;
        logLevel = logLevel == null ? LogLevel.INFO : logLevel;
        traceId = normalize(traceId, "");
        taskId = normalize(taskId, "");
        appId = normalize(appId, "");
        sceneCode = normalize(sceneCode, "");
        source = normalize(source, "");
        eventType = normalize(eventType, "");
        message = normalize(message, "");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    private static String normalize(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
