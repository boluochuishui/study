package org.example.study.baseSdk.chain;

public interface ChainExceptionHandler<C extends ChainContext> {

    ChainNodeResult handle(
            C context,
            ChainDefinition definition,
            ChainHandlerDefinition handlerDefinition,
            Exception exception
    );
}
