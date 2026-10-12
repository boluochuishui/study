package org.example.study.lifecycle;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/** 实际 HTTP 验证探针绕过业务鉴权，而下线请求仍受专用令牌保护。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "base-sdk.lifecycle.drain-token=lifecycle-http-test-token")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LifecycleHttpIntegrationTests {
    @LocalServerPort
    private int port;

    @Test
    void drainChangesReadinessAndRejectsBusinessTraffic() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            assertThat(send(client, "/internal/lifecycle/ready", false, "").statusCode()).isEqualTo(200);
            assertThat(send(client, "/internal/lifecycle/drain", true, "wrong").statusCode()).isEqualTo(403);
            assertThat(send(client, "/internal/lifecycle/ready", false, "").statusCode()).isEqualTo(200);
            assertThat(send(client, "/internal/lifecycle/drain", true, "lifecycle-http-test-token").statusCode()).isEqualTo(200);
            assertThat(send(client, "/internal/lifecycle/ready", false, "").statusCode()).isEqualTo(503);
            assertThat(send(client, "/internal/lifecycle/live", false, "").statusCode()).isEqualTo(200);
            var rejected = send(client, "/api/detect/sync", true, "");
            assertThat(rejected.statusCode()).isEqualTo(503);
            assertThat(rejected.body()).contains("SERVICE_UNAVAILABLE");
        }
    }

    private HttpResponse<String> send(HttpClient client, String path, boolean post, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).header("X-Drain-Token", token);
        if (post) {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
