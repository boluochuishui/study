package org.example.study.baseSdk.auth;

import java.time.Instant;
import java.util.List;

/**
 * 已发布的不可变鉴权规则快照。
 */
public record AuthConfigSnapshot(
        String version,
        Instant publishedAt,
        String publishedBy,
        List<AuthRule> rules,
        String checksum
) {
    public AuthConfigSnapshot {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }
}
