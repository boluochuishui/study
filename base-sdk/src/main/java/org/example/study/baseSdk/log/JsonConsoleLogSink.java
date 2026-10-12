package org.example.study.baseSdk.log;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** 向标准输出写入单行 JSON，供容器日志采集器读取。 */
@Component
@ConditionalOnProperty(name = "study.logs.structured.enabled", havingValue = "true")
public class JsonConsoleLogSink implements LogSink {

    private final ObjectMapper mapper;
    private final long tenantId;
    private final String serviceName;

    public JsonConsoleLogSink(ObjectMapper mapper, @Value("${study.logs.tenant-id:0}") long tenantId,
                              @Value("${spring.application.name:study-service}") String serviceName) {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("Structured log tenant ID must be positive");
        }
        this.mapper = mapper;
        this.tenantId = tenantId;
        this.serviceName = serviceName;
    }

    @Override
    public void record(BaseLogEvent event) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("@timestamp", event.occurredAt().toString());
        document.put("eventId", UUID.randomUUID().toString().replace("-", ""));
        document.put("tenantId", tenantId);
        document.put("serviceName", serviceName);
        document.put("logSpace", event.logSpace());
        document.put("logType", event.logType().name());
        document.put("level", event.logLevel().name());
        document.put("traceId", event.traceId());
        document.put("taskId", event.taskId());
        document.put("appId", event.appId());
        document.put("sceneCode", event.sceneCode());
        document.put("source", event.source());
        document.put("eventType", event.eventType());
        document.put("message", event.message());
        document.put("success", event.success());
        document.put("costMillis", event.costMillis());
        Object itemId = event.attributes().get("itemId");
        if (itemId instanceof String value) {
            document.put("itemId", value);
        }
        Object handlerName = event.attributes().get("handlerName");
        if (handlerName instanceof String value) {
            document.put("handlerName", value);
        }
        System.out.println(mapper.writeValueAsString(document));
    }
}
