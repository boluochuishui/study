package org.example.study.baseSdk.auth;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证通用签名协议和防重放边界。
 */
class SignatureAuthenticationProviderTests {

    private final HmacSignatureService signatureService = new HmacSignatureService();
    private final CredentialProvider credentialProvider = id -> "demo-app".equals(id)
            ? Optional.of(new Credential(id, "demo-secret", true, null))
            : Optional.empty();

    @Test
    void validSignatureCreatesPrincipal() {
        SignatureAuthenticationProvider provider = provider();
        AuthenticationRequest unsigned = request("nonce-valid", "");
        String timestamp = unsigned.headers().get("X-Timestamp");
        String signature = signatureService.sign("demo-secret",
                signatureService.canonicalPayload(unsigned, "demo-app", timestamp, "nonce-valid"));

        AuthenticationPrincipal principal = provider.authenticate(request(timestamp, "nonce-valid", signature), rule());

        assertThat(principal.subject()).isEqualTo("demo-app");
        assertThat(principal.permissions()).containsExactly("detect:sync");
    }

    @Test
    void duplicatedNonceIsRejected() {
        SignatureAuthenticationProvider provider = provider();
        AuthenticationRequest unsigned = request("nonce-replay", "");
        String timestamp = unsigned.headers().get("X-Timestamp");
        String signature = signatureService.sign("demo-secret",
                signatureService.canonicalPayload(unsigned, "demo-app", timestamp, "nonce-replay"));
        AuthenticationRequest signed = request(timestamp, "nonce-replay", signature);

        provider.authenticate(signed, rule());

        assertThatThrownBy(() -> provider.authenticate(signed, rule()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Request replay detected");
    }

    @Test
    void bodyTamperingInvalidatesSignature() {
        SignatureAuthenticationProvider provider = provider();
        AuthenticationRequest original = request("nonce-body", "");
        String timestamp = original.headers().get("X-Timestamp");
        String signature = signatureService.sign("demo-secret",
                signatureService.canonicalPayload(original, "demo-app", timestamp, "nonce-body"));
        AuthenticationRequest tampered = new AuthenticationRequest(
                "POST", "/api/detect/sync",
                headers(timestamp, "nonce-body", signature),
                "{\"text\":\"changed\"}".getBytes(StandardCharsets.UTF_8), "127.0.0.1", "HTTP");

        assertThatThrownBy(() -> provider.authenticate(tampered, rule()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid signature");
    }

    private SignatureAuthenticationProvider provider() {
        return new SignatureAuthenticationProvider(
                credentialProvider, new InMemoryNonceStore(), signatureService, 300, 600);
    }

    private AuthenticationRequest request(String nonce, String signature) {
        return request(String.valueOf(Instant.now().getEpochSecond()), nonce, signature);
    }

    private AuthenticationRequest request(String timestamp, String nonce, String signature) {
        return new AuthenticationRequest(
                "POST", "/api/detect/sync", headers(timestamp, nonce, signature),
                "{\"text\":\"normal\"}".getBytes(StandardCharsets.UTF_8), "127.0.0.1", "HTTP");
    }

    private Map<String, String> headers(String timestamp, String nonce, String signature) {
        return Map.of(
                "X-App-Id", "demo-app",
                "X-Timestamp", timestamp,
                "X-Nonce", nonce,
                "X-Signature", signature
        );
    }

    private AuthRule rule() {
        return new AuthRule("detect-sync", "同步检测", "/api/detect/sync", Set.of("POST"),
                AuthMode.SIGNATURE, Map.of("requiredPermission", "detect:sync"), 10, true);
    }
}
