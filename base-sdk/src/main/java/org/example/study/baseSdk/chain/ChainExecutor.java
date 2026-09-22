package org.example.study.baseSdk.chain;

/**
 * Executes an already assembled chain definition.
 */
public interface ChainExecutor<C extends ChainContext> {

    ChainExecuteResult execute(C context, ChainDefinition definition);
}
