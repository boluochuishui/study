package org.example.study.baseSdk.chain.support;

import org.example.study.baseSdk.chain.ChainContext;
import org.example.study.baseSdk.chain.ChainDefinition;
import org.example.study.baseSdk.chain.ChainExceptionHandler;
import org.example.study.baseSdk.chain.ChainExceptionPolicy;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Default exception adapter that keeps handler failures visible in chain results.
 */
@Component
public class DefaultChainExceptionHandler implements ChainExceptionHandler<ChainContext> {

    @Override
    public ChainNodeResult handle(ChainContext context, ChainDefinition definition, ChainHandlerDefinition handlerDefinition, Exception exception) {
        boolean breakChain = definition.exceptionPolicy() != ChainExceptionPolicy.CONTINUE;
        return new ChainNodeResult(
                handlerDefinition.handlerName(),
                "EXECUTE",
                false,
                breakChain,
                "HANDLER_EXCEPTION",
                exception.getMessage(),
                List.of("exception"),
                Map.of("exceptionClass", exception.getClass().getName()),
                0
        );
    }
}
