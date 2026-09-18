package org.example.study.security;

import org.example.study.domain.DetectRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Verifies HMAC-SHA256 request signatures without exposing secret keys on the wire.
 */
@Component
public class ApiSignatureVerifier {

    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 300;
    private final ApiCredentialRepository credentialRepository;

    public ApiSignatureVerifier(ApiCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    public void verify(String appId, String timestamp, String nonce, String signature, DetectRequest request) {
        if (isBlank(appId) || isBlank(timestamp) || isBlank(nonce) || isBlank(signature)) {
            throw new IllegalArgumentException("Missing authentication headers");
        }
        validateTimestamp(timestamp);
        String secret = credentialRepository.findSecretByAppId(appId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid app id"));
        String expected = sign(secret, canonicalPayload(appId, timestamp, nonce, request));
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid signature");
        }
    }

    public String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate signature", ex);
        }
    }

    public String canonicalPayload(String appId, String timestamp, String nonce, DetectRequest request) {
        return "appId=" + safe(appId)
                + "&timestamp=" + safe(timestamp)
                + "&nonce=" + safe(nonce)
                + "&clientRequestId=" + safe(request.clientRequestId())
                + "&sceneCode=" + safe(request.sceneCode())
                + "&contentType=" + safe(request.contentType())
                + "&text=" + safe(request.text())
                + "&imageUrl=" + safe(request.imageUrl())
                + "&audioUrl=" + safe(request.audioUrl())
                + "&videoUrl=" + safe(request.videoUrl());
    }

    private void validateTimestamp(String timestamp) {
        long requestEpochSecond;
        try {
            requestEpochSecond = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid timestamp");
        }
        if (Math.abs(Instant.now().getEpochSecond() - requestEpochSecond) > ALLOWED_CLOCK_SKEW_SECONDS) {
            throw new IllegalArgumentException("Expired request timestamp");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
