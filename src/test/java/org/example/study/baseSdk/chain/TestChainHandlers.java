package org.example.study.baseSdk.chain;

import org.springframework.stereotype.Component;

/**
 * Test-only handlers used by legacy chain runtime verification resources.
 */
class TestChainHandlers {
}

@Component("test.start.handler")
class TestStartHandler implements ChainHandler<ChainContext> {

    @Override
    public ChainNodeResult handle(ChainContext context, ChainHandlerDefinition handlerDefinition) {
        return ChainNodeResult.success(handlerDefinition.handlerName(), 1);
    }
}

@Component("test.middle.handler")
class TestMiddleHandler implements ChainHandler<ChainContext> {

    @Override
    public ChainNodeResult handle(ChainContext context, ChainHandlerDefinition handlerDefinition) {
        return ChainNodeResult.success(handlerDefinition.handlerName(), 1);
    }
}

@Component("test.finally.handler")
class TestFinallyHandler implements ChainHandler<ChainContext> {

    @Override
    public ChainNodeResult handle(ChainContext context, ChainHandlerDefinition handlerDefinition) {
        return ChainNodeResult.success(handlerDefinition.handlerName(), 1);
    }
}
