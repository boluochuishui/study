package org.example.study.baseSdk.chain;

/**
 * Contract reserved for MQ-backed asynchronous chain submission.
 */
public interface AsyncChainExecutor<C extends ChainContext> {

    ChainSubmitResult submit(String chainName, C context);
}
