package org.example.study.baseSdk.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证过滤器验签后请求体仍可进入检测 Controller，并确认重放请求被拒绝。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationFilterIntegrationTests {

    private static final String PATH = "/api/detect/sync";
    private static final String BODY = "{\"clientRequestId\":\"auth-test\",\"sceneCode\":\"comment\","
            + "\"contentType\":\"TEXT\",\"text\":\"normal content\"}";

    @LocalServerPort
    private int port;

    @Autowired
    private HmacSignatureService signatureService;

    @Test
    void signedRequestReachesControllerAndReplayIsRejected() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString();
        AuthenticationRequest authRequest = new AuthenticationRequest(
                "POST", PATH, Map.of(), BODY.getBytes(StandardCharsets.UTF_8), "127.0.0.1", "HTTP");
        String signature = signatureService.sign("demo-secret",
                signatureService.canonicalPayload(authRequest, "demo-app", timestamp, nonce));
        HttpRequest request = request(timestamp, nonce, signature);
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> first = client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> replay = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.body()).contains("\"taskId\":\"dt_");
        assertThat(replay.statusCode()).isEqualTo(401);
        assertThat(replay.body()).contains("AUTHENTICATION_FAILED", "Request replay detected");
    }

    @Test
    void missingAuthenticationHeadersAreRejectedBeforeController() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + PATH))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(BODY))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("AUTHENTICATION_FAILED", "Missing authentication header");
    }

    private HttpRequest request(String timestamp, String nonce, String signature) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + PATH))
                .header("Content-Type", "application/json")
                .header("X-App-Id", "demo-app")
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .header("X-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(BODY))
                .build();
    }
}
