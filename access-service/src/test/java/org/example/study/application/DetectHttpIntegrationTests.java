package org.example.study.application;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.example.study.baseSdk.auth.AuthenticationRequest;
import org.example.study.baseSdk.auth.HmacSignatureService;
import org.example.study.baseSdk.storage.DetectionFileStore;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.DetectCommand;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectItemResult;
import org.example.study.domain.DetectStatus;
import org.example.study.rpc.ImageDetectRpcClient;
import org.example.study.rpc.TextDetectRpcClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 使用真实 HTTP 和 Servlet multipart 解析验证签名及文件不会被鉴权层耗尽。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DetectHttpIntegrationTests {
    @LocalServerPort int port;
    @MockitoBean TextDetectRpcClient text;
    @MockitoBean ImageDetectRpcClient image;
    @Autowired DetectionFileStore store;

    @Test
    void synchronousMultipleItemsAreRejected() throws Exception {
        when(text.detect(any())).thenAnswer(invocation -> result(invocation.getArgument(0)));
        String json = """
                {"sceneCode":"comment","contentType":"TEXT","language":"zh-CN","region":"CN",
                 "items":[{"itemId":"one","text":"first"},{"itemId":"two","text":"second"}]}
                """;
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        var response = send("/api/detect/sync", "application/json", body, body);
        assertThat(response.statusCode()).isEqualTo(400);
        org.mockito.Mockito.verify(text, org.mockito.Mockito.never()).detect(any());
    }

    @Test
    void singleItemUsesSameResponseContractAndLegacyBodyIsRejected() throws Exception {
        when(text.detect(any())).thenAnswer(invocation -> result(invocation.getArgument(0)));
        byte[] body = """
                {"sceneCode":"comment","contentType":"TEXT","items":[{"itemId":"one","text":"single"}]}
                """.getBytes(StandardCharsets.UTF_8);
        var response = send("/api/detect/sync", "application/json", body, body);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"results\":[", "\"itemId\":\"one\"");
        byte[] legacy = "{\"sceneCode\":\"comment\",\"contentType\":\"TEXT\",\"text\":\"single\"}"
                .getBytes(StandardCharsets.UTF_8);
        assertThat(send("/api/detect/sync", "application/json", legacy, legacy).statusCode()).isEqualTo(400);
    }

    @Test
    void rejectsMultipartForBothDetectionRoutes() throws Exception {
        byte[] body = new byte[0];
        for (String path : List.of("/api/detect/sync", "/api/detect/async")) {
            var response = send(path, "multipart/form-data; boundary=disabled", body, body);
            assertThat(response.statusCode()).isEqualTo(415);
            assertThat(response.body()).contains("UNSUPPORTED_CONTENT_TYPE");
        }
    }

    @Test
    void rejectsTamperedJsonAndAsyncReturnsAcceptedOnlyAfterRpcAcceptance() throws Exception {
        when(text.submit(any())).thenAnswer(invocation -> {
            DetectCommand command = invocation.getArgument(0);
            return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(), DetectStatus.ACCEPTED,
                    null, command.submittedAt(), true, false, "", "");
        });
        byte[] body = """
                {"sceneCode":"comment","contentType":"TEXT","items":[{"itemId":"one","text":"text"},{"itemId":"two","text":"second"}]}
                """.getBytes(StandardCharsets.UTF_8);
        assertThat(send("/api/detect/async", "application/json", body, body).statusCode()).isEqualTo(202);
        org.mockito.Mockito.verify(text, org.mockito.Mockito.times(1)).submit(org.mockito.ArgumentMatchers.argThat(
                command -> command.items().size() == 2 && command.executionMode() == org.example.study.domain.DetectExecutionMode.ASYNC));
        assertThat(send("/api/detect/sync", "application/json", body, new byte[]{1}).statusCode()).isEqualTo(401);
    }

    @Test
    void base64JsonIsSignedAndRpcReceivesOnlyFileReference() throws Exception {
        var captured = new java.util.concurrent.atomic.AtomicReference<DetectCommand>();
        when(image.detect(any())).thenAnswer(invocation -> {
            DetectCommand command = invocation.getArgument(0);
            captured.set(command);
            assertThat(command.items().getFirst().url()).isNull();
            assertThat(store.resolve(command.items().getFirst().fileId())).exists();
            assertThat(new tools.jackson.databind.ObjectMapper().writeValueAsString(command))
                    .doesNotContain("\"data\"", "not-a-url");
            return result(command);
        });
        String data = java.util.Base64.getEncoder().encodeToString(DetectRequestValidatorTests.png());
        byte[] body = ("{\"sceneCode\":\"comment\",\"contentType\":\"IMAGE\",\"items\":[{\"itemId\":\"one\","
                + "\"url\":\"not-a-url\",\"data\":\"" + data + "\"}]}").getBytes(StandardCharsets.UTF_8);
        try {
            var response = send("/api/detect/sync", "application/json", body, body);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"itemId\":\"one\"");
            byte[] tampered = body.clone();
            tampered[tampered.length - 5] ^= 1;
            assertThat(send("/api/detect/sync", "application/json", tampered, body).statusCode()).isEqualTo(401);
        } finally {
            if (captured.get() != null) { store.delete(captured.get().items().getFirst().fileId()); }
        }
    }

    private DetectResult result(DetectCommand command) {
        return new DetectResult(command.taskId(), command.contentType(), "COMPLETED", command.items().stream()
                .map(item -> new DetectItemResult(item.itemId(), DetectStatus.SUCCEEDED, DetectAction.PASS, List.of(), false, "", "")).toList());
    }

    private HttpResponse<String> send(String path, String contentType, byte[] body, byte[] signedBody) throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString();
        var signer = new HmacSignatureService();
        var auth = new AuthenticationRequest("POST", path, Map.of(), signedBody, "127.0.0.1", "HTTP");
        String signature = signer.sign("demo-secret", signer.canonicalPayload(auth, "demo-app", timestamp, nonce));
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", contentType).header("X-App-Id", "demo-app").header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce).header("X-Signature", signature).POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        try (HttpClient client = HttpClient.newHttpClient()) { return client.send(request, HttpResponse.BodyHandlers.ofString()); }
    }

}
