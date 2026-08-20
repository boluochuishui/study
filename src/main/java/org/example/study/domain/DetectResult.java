package org.example.study.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record DetectResult(
        String requestId,
        String sceneCode,
        DetectAction action,
        List<String> labels,
        List<NodeResult> nodeResults,
        long costMillis
) {

    public static DetectResult from(DetectContext context, Instant startedAt, Instant endedAt) {
        List<String> labels = context.nodeResults().stream()
                .flatMap(result -> result.labels().stream())
                .distinct()
                .toList();

        return new DetectResult(
                context.requestId(),
                context.sceneCode(),
                context.maxRiskAction(),
                labels,
                context.nodeResults(),
                Duration.between(startedAt, endedAt).toMillis()
        );
    }
}
