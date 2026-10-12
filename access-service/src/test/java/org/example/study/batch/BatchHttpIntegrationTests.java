package org.example.study.batch;

import java.io.ByteArrayOutputStream;
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
import org.example.study.baseSdk.auth.MultipartSignatureBody;
import org.example.study.baseSdk.storage.DetectionFileStore;
import org.example.study.domain.AsyncDetectAcceptedResult;
import org.example.study.domain.BatchDetectCommand;
import org.example.study.domain.BatchDetectResult;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectItemResult;
import org.example.study.domain.DetectStatus;
import org.example.study.rpc.ImageDetectRpcClient;
import org.example.study.rpc.TextDetectRpcClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 使用真实 HTTP 和 Servlet multipart 解析验证签名及文件不会被鉴权层耗尽。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BatchHttpIntegrationTests {
    @LocalServerPort int port;
    @MockitoBean TextDetectRpcClient text;
    @MockitoBean ImageDetectRpcClient image;
    @Autowired DetectionFileStore store;

    @Test
    void signedJsonBatchReturnsOneTaskWithOrderedItems() throws Exception {
        when(text.detectBatch(any())).thenAnswer(invocation -> result(invocation.getArgument(0)));
        String json = """
                {"sceneCode":"comment","contentType":"TEXT","language":"zh-CN","region":"CN",
                 "items":[{"itemId":"one","text":"first"},{"itemId":"two","text":"second"}]}
                """;
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        var response = send("/api/detect/batch/sync", "application/json", body, body);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("COMPLETED", "\"itemId\":\"one\"", "\"itemId\":\"two\"");
    }

    @Test
    void signedBinaryUploadReachesRpcWithReadableReference() throws Exception {
        var captured = new java.util.concurrent.atomic.AtomicReference<BatchDetectCommand>();
        when(image.detectBatch(any())).thenAnswer(invocation -> {
            BatchDetectCommand command = invocation.getArgument(0);
            captured.set(command);
            assertThat(store.resolve(command.items().getFirst().fileId())).exists();
            return result(command);
        });
        byte[] file = BatchRequestPreparerTests.png();
        byte[] metadata = """
                {"sceneCode":"comment","contentType":"IMAGE","items":[{"itemId":"one","filePartName":"img"}]}
                """.getBytes(StandardCharsets.UTF_8);
        String boundary = "study-upload-boundary";
        var request = new MockHttpServletRequest();
        var meta = new MockPart("metadata", metadata);
        meta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        var binary = new MockPart("img", "a.png", file);
        binary.getHeaders().setContentType(MediaType.IMAGE_PNG);
        request.addPart(meta);
        request.addPart(binary);
        byte[] signed = MultipartSignatureBody.canonical(request, 67108864);
        try {
            byte[] tampered = file.clone();
            tampered[tampered.length - 1] ^= 1;
            assertThat(send("/api/detect/batch/sync", "multipart/form-data; boundary=" + boundary,
                    multipart(boundary, metadata, tampered), signed).statusCode()).isEqualTo(401);
            var response = send("/api/detect/batch/sync", "multipart/form-data; boundary=" + boundary,
                    multipart(boundary, metadata, file), signed);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(captured.get().items().getFirst().fileId()).isNotBlank();
            assertThat(captured.get().items().getFirst().errorCode()).isEmpty();
        } finally {
            if (captured.get() != null && captured.get().items().getFirst().fileId() != null) {
                store.delete(captured.get().items().getFirst().fileId());
            }
        }
    }

    @Test
    void rejectsTamperedJsonAndAsyncReturnsAcceptedOnlyAfterRpcAcceptance() throws Exception {
        when(text.submitBatch(any())).thenAnswer(invocation -> {
            BatchDetectCommand command = invocation.getArgument(0);
            return new AsyncDetectAcceptedResult(command.taskId(), command.clientRequestId(), DetectStatus.ACCEPTED,
                    null, command.submittedAt(), true, false, "", "");
        });
        byte[] body = """
                {"sceneCode":"comment","contentType":"TEXT","items":[{"itemId":"one","text":"text"}]}
                """.getBytes(StandardCharsets.UTF_8);
        assertThat(send("/api/detect/batch/async", "application/json", body, body).statusCode()).isEqualTo(202);
        assertThat(send("/api/detect/batch/sync", "application/json", body, new byte[]{1}).statusCode()).isEqualTo(401);
    }

    private BatchDetectResult result(BatchDetectCommand command) {
        return new BatchDetectResult(command.taskId(), command.contentType(), "COMPLETED", command.items().stream()
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

    private byte[] multipart(String boundary, byte[] metadata, byte[] file) throws Exception {
        var out = new ByteArrayOutputStream();
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"metadata\"\r\nContent-Type: application/json\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write(metadata);
        out.write(("\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"img\"; filename=\"a.png\"\r\nContent-Type: image/png\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write(file);
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
