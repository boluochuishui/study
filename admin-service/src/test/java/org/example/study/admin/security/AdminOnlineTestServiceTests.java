package org.example.study.admin.security;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.example.study.admin.api.AdminApiException;
import org.example.study.admin.api.OnlineTestRequest;
import org.example.study.admin.service.AdminOnlineTestService;
import org.example.study.admin.service.OnlineTestProperties;
import org.example.study.baseSdk.auth.AuthenticationRequest;
import org.example.study.baseSdk.auth.HmacSignatureService;
import org.example.study.baseSdk.database.api.model.ConfigItemData;
import org.example.study.baseSdk.database.api.model.OnlineTestRecordData;
import org.example.study.baseSdk.database.api.service.ConfigDataService;
import org.example.study.baseSdk.database.api.service.OnlineTestRecordDataService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 在线测试必须走带签名的接入入口，并保持记录不含样本。 */
class AdminOnlineTestServiceTests {
    @Test
    void signsRequestAndStoresMetadataOnly() throws Exception {
        AtomicBoolean signatureValid = new AtomicBoolean();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/detect/sync", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            var headers = exchange.getRequestHeaders();
            String appId = headers.getFirst("X-App-Id");
            String timestamp = headers.getFirst("X-Timestamp");
            String nonce = headers.getFirst("X-Nonce");
            HmacSignatureService signer = new HmacSignatureService();
            String expected = signer.sign("test-secret", signer.canonicalPayload(new AuthenticationRequest(
                    "POST", "/api/detect/sync", Map.of(), body, "", "HTTP"), appId, timestamp, nonce));
            signatureValid.set(expected.equals(headers.getFirst("X-Signature")));
            byte[] response = "{\"taskId\":\"task-1\",\"status\":\"COMPLETED\",\"results\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            ConfigDataService configs = mock(ConfigDataService.class);
            OnlineTestRecordDataService records = mock(OnlineTestRecordDataService.class);
            when(configs.findEnabled(7L, "console.online-test", "TEXT")).thenReturn(Optional.of(
                    new ConfigItemData(1L, 7L, "console.online-test", "TEXT",
                            "{\"appId\":\"test-app\",\"sceneCode\":\"text.default\"}", 1L,
                            true, "", LocalDateTime.now(), LocalDateTime.now())));
            OnlineTestProperties properties = new OnlineTestProperties();
            properties.setAccessUrl("http://127.0.0.1:" + server.getAddress().getPort());
            OnlineTestProperties.Credential credential = new OnlineTestProperties.Credential();
            credential.setAppId("test-app");
            credential.setSecret("test-secret");
            properties.setCredentials(Map.of("tenant-a", credential));
            AdminOnlineTestService service = new AdminOnlineTestService(configs, records,
                    new ObjectMapper(), properties);
            AdminSecurityContext.set(new AdminPrincipal(9L, 7L, "tenant-a", "tester", "Tester",
                    "hash", Set.of("TEST_RUN")));
            try {
                var result = service.run(new OnlineTestRequest("TEXT", "SYNC", "zh", "CN",
                        List.of(new OnlineTestRequest.OnlineTestItem("1", "private sample", null, null))));
                assertThat(result.taskId()).isEqualTo("task-1");
                assertThat(signatureValid.get()).isTrue();
                org.mockito.ArgumentCaptor<OnlineTestRecordData> captured =
                        org.mockito.ArgumentCaptor.forClass(OnlineTestRecordData.class);
                verify(records).save(captured.capture());
                assertThat(captured.getValue().resultJson()).doesNotContain("private sample");
                assertThat(captured.getValue().tenantId()).isEqualTo(7L);
                service.records();
                verify(records).list(7L, 100);
            } finally {
                AdminSecurityContext.clear();
            }
            AdminSecurityContext.set(new AdminPrincipal(9L, 8L, "tenant-b", "tester", "Tester",
                    "hash", Set.of("TEST_RUN")));
            try {
                when(configs.findEnabled(8L, "console.online-test", "TEXT")).thenReturn(Optional.of(
                        new ConfigItemData(2L, 8L, "console.online-test", "TEXT",
                                "{\"appId\":\"test-app\",\"sceneCode\":\"text.default\"}", 1L,
                                true, "", LocalDateTime.now(), LocalDateTime.now())));
                assertThatThrownBy(() -> service.run(new OnlineTestRequest("TEXT", "SYNC", "zh", "CN",
                        List.of(new OnlineTestRequest.OnlineTestItem("1", "x", null, null)))))
                        .isInstanceOf(AdminApiException.class).hasMessageContaining("credential unavailable");
            } finally {
                AdminSecurityContext.clear();
            }
        } finally {
            server.stop(0);
        }
    }
}
