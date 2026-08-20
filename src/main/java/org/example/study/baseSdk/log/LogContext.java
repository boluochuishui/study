package org.example.study.baseSdk.log;

import java.util.LinkedHashMap;
import java.util.Map;

public record LogContext(
        String traceId,
        String taskId,
        String appId,
        String sceneCode,
        Map<String, Object> attributes
) {

    public static LogContext empty() {
        return new LogContext("", "", "", "", Map.of());
    }

    public LogContext withAttribute(String key, Object value) {
        Map<String, Object> copied = new LinkedHashMap<>(attributes);
        copied.put(key, value);
        return new LogContext(traceId, taskId, appId, sceneCode, Map.copyOf(copied));
    }
}
