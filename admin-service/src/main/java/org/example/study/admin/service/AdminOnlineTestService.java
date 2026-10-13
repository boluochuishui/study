package org.example.study.admin.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.example.study.admin.api.AdminApiException;
import org.example.study.admin.api.OnlineTestProfile;
import org.example.study.admin.api.OnlineTestRequest;
import org.example.study.admin.api.OnlineTestRunResult;
import org.example.study.admin.security.AdminPrincipal;
import org.example.study.admin.security.AdminSecurityContext;
import org.example.study.baseSdk.auth.AuthenticationRequest;
import org.example.study.baseSdk.auth.HmacSignatureService;
import org.example.study.baseSdk.database.api.model.ConfigItemData;
import org.example.study.baseSdk.database.api.model.OnlineTestRecordData;
import org.example.study.baseSdk.database.api.service.ConfigDataService;
import org.example.study.baseSdk.database.api.service.OnlineTestRecordDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 管理台在线测试编排；样本只在内存中转发，不进入记录或日志。 */
@Service
public class AdminOnlineTestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminOnlineTestService.class);
    private static final String NAMESPACE = "console.online-test";
    private static final int MAX_RESPONSE_BYTES = 1_048_576;

    private final ConfigDataService configs;
    private final OnlineTestRecordDataService records;
    private final ObjectMapper objectMapper;
    private final HttpClient http;
    private final HmacSignatureService signatures = new HmacSignatureService();
    private final URI accessBase;
    private final Map<String, Credential> credentials;

    private record Credential(String appId, String secret) {
    }

    public AdminOnlineTestService(ConfigDataService configs, OnlineTestRecordDataService records,
                                  ObjectMapper json, OnlineTestProperties properties) {
        this.configs = configs;
        this.records = records;
        this.objectMapper = json;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        this.credentials = properties.getCredentials().entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        entry -> new Credential(entry.getValue().getAppId(), entry.getValue().getSecret())));
        this.accessBase = URI.create(properties.getAccessUrl());
        if (!List.of("http", "https").contains(accessBase.getScheme())
                || accessBase.getHost() == null || accessBase.getUserInfo() != null
                || accessBase.getRawQuery() != null || accessBase.getRawFragment() != null
                || !"".equals(accessBase.getPath()) && !"/".equals(accessBase.getPath())) {
            throw new IllegalArgumentException("Invalid online test access URL");
        }
    }

    public OnlineTestProfile profile(String contentType) {
        String type = checkedType(contentType);
        AdminPrincipal principal = AdminSecurityContext.requirePrincipal();
        ConfigItemData item = configs.findEnabled(principal.tenantId(), NAMESPACE, type)
                .orElseThrow(() -> new AdminApiException("TEST_PROFILE_MISSING",
                        "Online test profile not configured", HttpStatus.CONFLICT));
        try {
            OnlineTestProfile profile = objectMapper.readValue(item.configValue(), OnlineTestProfile.class);
            if (profile.appId() == null || profile.appId().isBlank()
                    || profile.sceneCode() == null || profile.sceneCode().isBlank()) {
                throw new IllegalArgumentException("Incomplete online test profile");
            }
            return profile;
        } catch (RuntimeException exception) {
            throw new AdminApiException("TEST_PROFILE_INVALID", "Invalid online test profile",
                    HttpStatus.CONFLICT);
        }
    }

    public OnlineTestRunResult run(OnlineTestRequest request) {
        validate(request);
        AdminPrincipal principal = AdminSecurityContext.requirePrincipal();
        OnlineTestProfile profile = profile(request.contentType());
        Credential credential = credentials.get(principal.tenantCode());
        if (credential == null || !profile.appId().equals(credential.appId())
                || credential.secret() == null || credential.secret().isBlank()) {
            throw new AdminApiException("TEST_CREDENTIAL_UNAVAILABLE", "Online test credential unavailable",
                    HttpStatus.CONFLICT);
        }
        String path = "/api/detect/" + request.executionMode().toLowerCase(java.util.Locale.ROOT);
        byte[] body = objectMapper.writeValueAsBytes(Map.of(
                "clientRequestId", UUID.randomUUID().toString(),
                "sceneCode", profile.sceneCode(), "contentType", request.contentType(),
                "language", request.language() == null ? "" : request.language(),
                "region", request.region() == null ? "" : request.region(), "items", request.items()));
        if (body.length > 60 * 1024 * 1024) {
            throw new AdminApiException("INVALID_REQUEST", "Online test request too large", HttpStatus.BAD_REQUEST);
        }
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString();
        String signature = signatures.sign(credential.secret(), signatures.canonicalPayload(
                new AuthenticationRequest("POST", path, Map.of(), body, "", "HTTP"),
                profile.appId(), timestamp, nonce));
        HttpRequest upstream = HttpRequest.newBuilder(accessBase.resolve(path))
                .timeout(Duration.ofSeconds(40)).header("Content-Type", "application/json")
                .header("X-App-Id", profile.appId()).header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce).header("X-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        long start = System.nanoTime();
        String taskId = null;
        String status = "FAILED";
        String resultJson = null;
        try {
            HttpResponse<byte[]> response = http.send(upstream, HttpResponse.BodyHandlers.ofByteArray());
            if (response.body().length > MAX_RESPONSE_BYTES) {
                throw new AdminApiException("TEST_UPSTREAM_FAILED", "Online test response too large",
                        HttpStatus.BAD_GATEWAY);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("Online test upstream rejected, tenantId={}, status={}",
                        principal.tenantId(), response.statusCode());
                throw new AdminApiException("TEST_UPSTREAM_FAILED", "Online test upstream rejected request",
                        HttpStatus.BAD_GATEWAY);
            }
            JsonNode result = objectMapper.readTree(response.body());
            taskId = result.path("taskId").asText(null);
            status = result.path("status").asText("UNKNOWN");
            resultJson = objectMapper.writeValueAsString(result);
            return new OnlineTestRunResult(taskId, status, request.executionMode(),
                    elapsed(start), result);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AdminApiException("TEST_UPSTREAM_FAILED", "Online test interrupted",
                    HttpStatus.BAD_GATEWAY);
        } catch (java.io.IOException exception) {
            throw new AdminApiException("TEST_UPSTREAM_FAILED", "Online test upstream unavailable",
                    HttpStatus.BAD_GATEWAY);
        } finally {
            records.save(new OnlineTestRecordData(null, principal.tenantId(), principal.userId(),
                    request.contentType(), request.executionMode(), profile.appId(), profile.sceneCode(),
                    request.items().size(), taskId, status, resultJson, elapsed(start), LocalDateTime.now()));
        }
    }

    public List<OnlineTestRecordData> records() {
        return records.list(AdminSecurityContext.requirePrincipal().tenantId(), 100);
    }

    private void validate(OnlineTestRequest request) {
        if (request == null || request.contentType() == null || request.executionMode() == null
                || request.items() == null || request.items().isEmpty()) {
            throw new AdminApiException("INVALID_REQUEST", "Invalid online test request", HttpStatus.BAD_REQUEST);
        }
        String type = checkedType(request.contentType());
        boolean sync = "SYNC".equals(request.executionMode());
        if (!sync && !"ASYNC".equals(request.executionMode())
                || sync && (!List.of("TEXT", "IMAGE").contains(type) || request.items().size() != 1)
                || request.items().size() > (List.of("TEXT", "IMAGE").contains(type) ? 100 : 1)) {
            throw new AdminApiException("INVALID_REQUEST", "Unsupported online test mode or item count",
                    HttpStatus.BAD_REQUEST);
        }
        for (OnlineTestRequest.OnlineTestItem item : request.items()) {
            if (item == null || item.text() != null && item.text().length() > 5000
                    || item.data() != null && item.data().length() > ("IMAGE".equals(type) ? 14_000_000 : 70_000_000)
                    || item.url() != null && item.url().length() > 2048
                    || (item.text() == null || item.text().isBlank())
                    && (item.url() == null || item.url().isBlank())
                    && (item.data() == null || item.data().isBlank())) {
                throw new AdminApiException("INVALID_REQUEST", "Invalid online test item", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private String checkedType(String type) {
        if (type == null || !List.of("TEXT", "IMAGE", "AUDIO", "VIDEO").contains(type)) {
            throw new AdminApiException("INVALID_REQUEST", "Invalid online test content type", HttpStatus.BAD_REQUEST);
        }
        return type;
    }

    private long elapsed(long start) {
        return Duration.ofNanos(System.nanoTime() - start).toMillis();
    }
}
