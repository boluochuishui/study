package org.example.study.baseSdk.chain;

/**
 * Observability hook invoked around chain and handler execution.
 */
public interface ChainExecutionListener<C extends ChainContext> {

    void beforeChain(C context, ChainDefinition definition);

    void beforeHandler(C context, ChainDefinition definition, ChainHandlerDefinition handlerDefinition, String role);

    void afterHandler(C context, ChainDefinition definition, ChainHandlerDefinition handlerDefinition, ChainNodeResult result);

    void onHandlerException(C context, ChainDefinition definition, ChainHandlerDefinition handlerDefinition, Exception exception);

    void afterChain(C context, ChainDefinition definition, ChainExecuteResult result);
}
