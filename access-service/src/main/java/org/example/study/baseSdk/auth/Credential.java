package org.example.study.baseSdk.auth;

import java.time.Instant;

/**
 * 可由环境变量、数据库加密存储或 STS 提供的调用凭证。
 */
public record Credential(
        String credentialId,
        String secret,
        boolean enabled,
        Instant expireAt
) {
    public boolean isAvailable(Instant now) {
        return enabled && (expireAt == null || expireAt.isAfter(now));
    }
}
