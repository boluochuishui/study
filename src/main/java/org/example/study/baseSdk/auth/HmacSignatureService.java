package org.example.study.baseSdk.auth;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 提供与业务 DTO 无关的 HMAC-SHA256 签名计算。
 */
@Component
public class HmacSignatureService {

    public String canonicalPayload(AuthenticationRequest request, String appId, String timestamp, String nonce) {
        return request.method().toUpperCase() + '\n'
                + request.path() + '\n'
                + appId + '\n'
                + timestamp + '\n'
                + nonce + '\n'
                + sha256(request.body());
    }

    public String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to calculate signature", exception);
        }
    }

    public boolean matches(String expectedHex, String actualHex) {
        try {
            return MessageDigest.isEqual(
                    HexFormat.of().parseHex(expectedHex),
                    HexFormat.of().parseHex(actualHex)
            );
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String sha256(byte[] body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to calculate body digest", exception);
        }
    }
}
