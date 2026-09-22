package org.example.study.baseSdk.chain;

/**
 * Converts handler exceptions into structured node results.
 */
public interface ChainExceptionHandler<C extends ChainContext> {

    ChainNodeResult handle(C context, ChainDefinition definition, ChainHandlerDefinition handlerDefinition, Exception exception);
}
