package org.example.study.admin.service;

import org.example.study.admin.api.AdminApiException;
import org.example.study.admin.security.AdminSecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Elasticsearch 日志检索，只返回当前登录租户的数据。 */
@Service
public class AdminLogSearchService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_TASK_EVENTS = 500;
    private static final Duration MAX_RANGE = Duration.ofDays(30);
    private final RestClient client;
    private final String dataStream;
    private final boolean configured;

    public AdminLogSearchService(@Value("${admin.logs.elasticsearch.url:}") String url,
                                 @Value("${admin.logs.elasticsearch.api-key:}") String apiKey,
                                 @Value("${admin.logs.elasticsearch.data-stream:logs-contentrisk-*}") String dataStream) {
        this.configured = url != null && !url.isBlank();
        if (dataStream == null || !dataStream.matches("[a-z0-9.*_-]{1,128}")) {
            throw new IllegalArgumentException("Invalid log data stream");
        }
        this.dataStream = dataStream;
        RestClient.Builder builder = RestClient.builder().baseUrl(configured ? url : "http://localhost:9200");
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        builder.requestFactory(factory);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("Authorization", "ApiKey " + apiKey);
        }
        this.client = builder.build();
    }

    public Map<String, Object> search(Instant from, Instant to, int size, String cursor,
                                       String taskId, String traceId, String appId,
                                       String serviceName, String logSpace, String level, Boolean success) {
        requireRange(from, to);
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid page size");
        }
        List<Object> filters = filters(from, to);
        addTerm(filters, "taskId", taskId);
        addTerm(filters, "traceId", traceId);
        addTerm(filters, "appId", appId);
        addTerm(filters, "serviceName", serviceName);
        addTerm(filters, "logSpace", logSpace);
        addTerm(filters, "level", level);
        if (success != null) {
            filters.add(Map.of("term", Map.of("success", success)));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", Map.of("bool", Map.of("filter", filters)));
        body.put("sort", List.of(Map.of("@timestamp", Map.of("order", "desc",
                "format", "strict_date_optional_time")), Map.of("eventId", "asc")));
        body.put("size", size + 1);
        if (cursor != null && !cursor.isBlank()) {
            body.put("search_after", decodeCursor(cursor));
        }
        List<Map<String, Object>> hits = execute(body);
        boolean more = hits.size() > size;
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < Math.min(size, hits.size()); i++) {
            items.add(source(hits.get(i)));
        }
        String next = more ? encodeCursor(hits.get(size - 1)) : null;
        return Map.of("items", items, "hasMore", more, "nextCursor", next == null ? "" : next);
    }

    public Map<String, Object> task(String taskId, Instant from, Instant to) {
        if (taskId == null || !taskId.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("Invalid task ID");
        }
        requireRange(from, to);
        List<Object> filters = filters(from, to);
        filters.add(Map.of("term", Map.of("taskId", taskId)));
        Map<String, Object> body = Map.of(
                "query", Map.of("bool", Map.of("filter", filters)),
                "sort", List.of(Map.of("@timestamp", Map.of("order", "asc",
                        "format", "strict_date_optional_time")), Map.of("eventId", "asc")),
                "size", MAX_TASK_EVENTS + 1);
        List<Map<String, Object>> hits = execute(body);
        List<Map<String, Object>> events = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_TASK_EVENTS, hits.size()); i++) {
            events.add(source(hits.get(i)));
        }
        return Map.of("taskId", taskId, "events", events, "truncated", hits.size() > MAX_TASK_EVENTS);
    }

    private List<Object> filters(Instant from, Instant to) {
        List<Object> filters = new ArrayList<>();
        filters.add(Map.of("term", Map.of("tenantId", AdminSecurityContext.requirePrincipal().tenantId())));
        filters.add(Map.of("range", Map.of("@timestamp", Map.of("gte", from.toString(), "lte", to.toString()))));
        return filters;
    }

    private void addTerm(List<Object> filters, String field, String value) {
        if (value != null && !value.isBlank()) {
            if (value.length() > 128) {
                throw new IllegalArgumentException("Invalid log filter");
            }
            filters.add(Map.of("term", Map.of(field, value)));
        }
    }

    private void requireRange(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to) || Duration.between(from, to).compareTo(MAX_RANGE) > 0
                || to.isAfter(Instant.now().plus(Duration.ofMinutes(5)))) {
            throw new IllegalArgumentException("Invalid log time range");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> execute(Map<String, Object> body) {
        if (!configured) {
            throw unavailable(null);
        }
        try {
            Map<String, Object> response = client.post().uri("/{stream}/_search", dataStream)
                    .body(body).retrieve().body(Map.class);
            if (response == null || !(response.get("hits") instanceof Map<?, ?> hits)
                    || !(hits.get("hits") instanceof List<?> items)) {
                throw new IllegalStateException("Invalid log search response");
            }
            return (List<Map<String, Object>>) items;
        } catch (RestClientException | ClassCastException | IllegalStateException exception) {
            throw unavailable(exception);
        }
    }

    private AdminApiException unavailable(Exception cause) {
        return new AdminApiException("LOG_BACKEND_UNAVAILABLE", "Log search backend unavailable",
                HttpStatus.SERVICE_UNAVAILABLE, cause);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> source(Map<String, Object> hit) {
        Map<String, Object> raw = (Map<String, Object>) hit.get("_source");
        Map<String, Object> safe = new LinkedHashMap<>();
        for (String field : List.of("eventId", "@timestamp", "serviceName", "logSpace", "logType", "level",
                "traceId", "taskId", "itemId", "appId", "sceneCode", "source", "eventType",
                "message", "success", "costMillis", "handlerName")) {
            if (raw.containsKey(field)) {
                safe.put(field, raw.get(field));
            }
        }
        return safe;
    }

    @SuppressWarnings("unchecked")
    private String encodeCursor(Map<String, Object> hit) {
        List<Object> sort = (List<Object>) hit.get("sort");
        String plain = sort.get(0) + "\n" + sort.get(1);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    private List<String> decodeCursor(String cursor) {
        if (cursor.length() > 512) {
            throw new IllegalArgumentException("Invalid log cursor");
        }
        try {
            String plain = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = plain.split("\n", -1);
            if (parts.length != 2 || !parts[1].matches("[a-f0-9]{32}")) {
                throw new IllegalArgumentException("Invalid log cursor");
            }
            Instant.parse(parts[0]);
            return List.of(parts);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid log cursor", exception);
        }
    }
}
