package org.example.study.baseSdk.chain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChainContext {

    private final String traceId;
    private final String taskId;
    private final Instant startedAt;
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private ChainLogContext logContext;

    public ChainContext(String traceId, String taskId) {
        this.traceId = traceId;
        this.taskId = taskId;
        this.startedAt = Instant.now();
        this.logContext = ChainLogContext.empty(traceId, taskId);
    }

    public String traceId() {
        return traceId;
    }

    public String taskId() {
        return taskId;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public ChainLogContext logContext() {
        return logContext;
    }

    public void setLogContext(ChainLogContext logContext) {
        this.logContext = logContext == null ? ChainLogContext.empty(traceId, taskId) : logContext;
    }

    public void putAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public Map<String, Object> attributes() {
        return Map.copyOf(attributes);
    }
}
