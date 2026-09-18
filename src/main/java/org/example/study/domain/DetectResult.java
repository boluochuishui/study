package org.example.study.domain;

import org.example.study.baseSdk.chain.ChainExecuteResult;
import org.example.study.baseSdk.chain.ChainNodeResult;

import java.util.Comparator;
import java.util.List;

/**
 * 同步检测的对外结果，不暴露责任链节点等内部执行过程。
 */
public record DetectResult(
        String taskId,
        DetectStatus detectStatus,
        DetectAction action,
        List<String> labels,
        boolean degraded,
        String errorCode,
        String errorMessage
) {

    public DetectResult {
        labels = labels == null ? List.of() : List.copyOf(labels);
        errorCode = errorCode == null ? "" : errorCode;
        errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public static DetectResult success(ChainExecuteResult chainResult) {
        List<String> labels = chainResult.nodeResults().stream()
                .flatMap(result -> result.tags().stream())
                .distinct()
                .toList();
        DetectAction action = chainResult.nodeResults().stream()
                .map(DetectResult::actionOf)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(DetectAction.PASS);
        return new DetectResult(chainResult.taskId(), DetectStatus.SUCCEEDED, action, labels, false, "", "");
    }

    public static DetectResult degradedPass(String taskId, String errorCode, String errorMessage) {
        return new DetectResult(taskId, DetectStatus.FAILED, DetectAction.PASS, List.of(), true, errorCode, errorMessage);
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
