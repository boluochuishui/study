package org.example.study.baseSdk.chain;

import java.util.List;

public record ChainDefinition(
        String chainName,
        String version,
        ChainExecuteMode executeMode,
        ChainExceptionPolicy exceptionPolicy,
        HandlerReference timeoutExceptionHandler,
        List<ChainHandlerDefinition> executeHandlers,
        ChainHandlerDefinition finallyHandler
) {
}
