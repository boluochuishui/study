package org.example.study.baseSdk.chain;

public interface AsyncChainExecutor<C extends ChainContext> {

    ChainSubmitResult submit(String chainName, C context);
}
