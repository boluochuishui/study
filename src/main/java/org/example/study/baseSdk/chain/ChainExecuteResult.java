package org.example.study.baseSdk.chain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Structured result returned by a full chain execution.
 */
public record ChainExecuteResult(
        String traceId,
        String taskId,
        String chainName,
        boolean success,
        List<ChainNodeResult> nodeResults,
        long costMillis
) {

    public static ChainExecuteResult from(ChainContext context, List<ChainNodeResult> nodeResults, Instant endedAt) {
        boolean success = nodeResults.stream().allMatch(ChainNodeResult::success);
        return new ChainExecuteResult(
                context.traceId(),
                context.taskId(),
                context.logContext().chainName(),
                success,
                List.copyOf(nodeResults),
                Duration.between(context.startedAt(), endedAt).toMillis()
        );
    }
}
