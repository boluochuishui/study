package org.example.study.worker;

import org.example.study.baseSdk.chain.ChainExecuteResult;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.example.study.domain.DetectAction;
import org.example.study.domain.DetectResult;
import org.example.study.domain.DetectStatus;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * 将内部责任链结果转换为稳定的 RPC 结果。
 */
@Component
public class DetectResultMapper {

    public DetectResult from(ChainExecuteResult chainResult) {
        var labels = chainResult.nodeResults().stream()
                .flatMap(result -> result.tags().stream()).distinct().toList();
        DetectAction action = chainResult.nodeResults().stream()
                .map(this::actionOf)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(DetectAction.PASS);
        return new DetectResult(chainResult.taskId(), DetectStatus.SUCCEEDED, action, labels, false, "", "");
    }

    private DetectAction actionOf(ChainNodeResult result) {
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
