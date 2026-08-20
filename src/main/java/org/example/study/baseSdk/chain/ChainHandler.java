package org.example.study.baseSdk.chain;

public interface ChainHandler<C extends ChainContext> {

    ChainNodeResult handle(C context, ChainHandlerDefinition handlerDefinition);
}
