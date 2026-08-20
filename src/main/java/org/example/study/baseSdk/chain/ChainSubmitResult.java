package org.example.study.baseSdk.chain;

public record ChainSubmitResult(
        String taskId,
        String chainName,
        String status
) {
}
