package org.example.study.baseSdk.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

/**
 * 校验长期 AK/SK 请求签名，并使用 nonce 阻止窗口期内的请求重放。
 */
@Component
public class SignatureAuthenticationProvider implements AuthenticationProvider {

    private final CredentialProvider credentialProvider;
    private final NonceStore nonceStore;
    private final HmacSignatureService signatureService;
    private final long clockSkewSeconds;
    private final long nonceTtlSeconds;

    public SignatureAuthenticationProvider(
            CredentialProvider credentialProvider,
            NonceStore nonceStore,
            HmacSignatureService signatureService,
            @Value("${study.auth.clock-skew-seconds:300}") long clockSkewSeconds,
            @Value("${study.auth.nonce-ttl-seconds:600}") long nonceTtlSeconds
    ) {
        this.credentialProvider = credentialProvider;
        this.nonceStore = nonceStore;
        this.signatureService = signatureService;
        this.clockSkewSeconds = clockSkewSeconds;
        this.nonceTtlSeconds = nonceTtlSeconds;
    }

    @Override
    public AuthMode supportMode() {
        return AuthMode.SIGNATURE;
    }

    @Override
    public AuthenticationPrincipal authenticate(AuthenticationRequest request, AuthRule rule) {
        String appId = requiredHeader(request, "X-App-Id");
        String timestamp = requiredHeader(request, "X-Timestamp");
        String nonce = requiredHeader(request, "X-Nonce");
        String signature = requiredHeader(request, "X-Signature");
        long requestTime = parseAndValidateTimestamp(timestamp);

        Credential credential = credentialProvider.load(appId)
                .filter(item -> item.isAvailable(Instant.now()))
                .orElseThrow(() -> unauthorized("Invalid credential"));
        String payload = signatureService.canonicalPayload(request, appId, timestamp, nonce);
        String expected = signatureService.sign(credential.secret(), payload);
        if (!signatureService.matches(expected, signature)) {
            throw unauthorized("Invalid signature");
        }
        long expireAt = Math.max(requestTime + clockSkewSeconds, Instant.now().getEpochSecond() + nonceTtlSeconds);
        if (!nonceStore.acquire(appId, nonce, expireAt)) {
            throw unauthorized("Request replay detected");
        }
        return new AuthenticationPrincipal(appId, AuthMode.SIGNATURE, appId, permissions(rule));
    }

    private long parseAndValidateTimestamp(String timestamp) {
        try {
            long value = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - value) > clockSkewSeconds) {
                throw unauthorized("Request timestamp expired");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw unauthorized("Invalid timestamp");
        }
    }

    private String requiredHeader(AuthenticationRequest request, String name) {
        String value = request.headers().get(name);
        if (value == null || value.isBlank()) {
            throw unauthorized("Missing authentication header: " + name);
        }
        return value;
    }

    private Set<String> permissions(AuthRule rule) {
        String permission = rule.options().get("requiredPermission");
        return permission == null || permission.isBlank() ? Set.of() : Set.of(permission);
    }

    private AuthenticationException unauthorized(String message) {
        return new AuthenticationException("AUTHENTICATION_FAILED", message, 401);
    }
}
