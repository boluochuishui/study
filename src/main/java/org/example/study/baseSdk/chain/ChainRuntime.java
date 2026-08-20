package org.example.study.baseSdk.chain;

public interface ChainRuntime {

    <C extends ChainContext> ChainExecuteResult execute(String chainName, C context);

    <C extends ChainContext> ChainSubmitResult submit(String chainName, C context);
}
