package org.example.study.text.cache;

import org.example.study.domain.DetectAction;

import java.time.Instant;
import java.util.List;

/** Redis 中保存的最小违规结论，不包含样本原文。 */
public record CachedTextViolation(
        DetectAction action,
        List<String> labels,
        String decisionVersion,
        Instant cachedAt
) {
    public CachedTextViolation {
        labels = labels == null ? List.of() : List.copyOf(labels);
    }
}
