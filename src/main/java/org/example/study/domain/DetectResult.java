package org.example.study.domain;

import org.example.study.baseSdk.chain.ChainExecuteResult;
import org.example.study.baseSdk.chain.ChainNodeResult;

import java.util.Comparator;
import java.util.List;

/**
 * API response returned after a moderation chain finishes.
 */
public record DetectResult(
        String taskId,
        String chainName,
        DetectAction action,
        List<String> labels,
        List<ChainNodeResult> nodeResults,
        long costMillis
) {

    public static DetectResult from(ChainExecuteResult chainResult) {
        List<String> labels = chainResult.nodeResults().stream()
                .flatMap(result -> result.tags().stream())
                .distinct()
                .toList();
        DetectAction action = chainResult.nodeResults().stream()
                .map(DetectResult::actionOf)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(DetectAction.PASS);
        return new DetectResult(chainResult.taskId(), chainResult.chainName(), action, labels, chainResult.nodeResults(), chainResult.costMillis());
    }

    private static DetectAction actionOf(ChainNodeResult result) {
        if (!result.success()) {
            return DetectAction.REVIEW;
        }
        return switch (result.resultCode()) {
            case "REJECT" -> DetectAction.REJECT;
            case "REVIEW" -> DetectAction.REVIEW;
            default -> DetectAction.PASS;
        };
    }
}
