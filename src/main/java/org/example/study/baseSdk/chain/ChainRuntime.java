package org.example.study.baseSdk.chain;

/**
 * Unified entry point for selecting and executing a configured chain.
 */
public interface ChainRuntime {

    <C extends ChainContext> ChainExecuteResult execute(String chainName, C context);

    <C extends ChainContext> ChainSubmitResult submit(String chainName, C context);
}
