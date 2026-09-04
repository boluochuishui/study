package org.example.study.baseSdk.chain;

/**
 * Business extension point for a single responsibility-chain node.
 */
public interface ChainHandler<C extends ChainContext> {

    ChainNodeResult handle(C context, ChainHandlerDefinition handlerDefinition);
}
