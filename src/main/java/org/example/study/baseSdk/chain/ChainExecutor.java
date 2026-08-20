package org.example.study.baseSdk.chain;

public interface ChainExecutor<C extends ChainContext> {

    ChainExecuteResult execute(C context, ChainDefinition definition);
}
