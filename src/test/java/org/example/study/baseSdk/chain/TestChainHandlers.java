package org.example.study.baseSdk.chain;

import org.springframework.stereotype.Component;

@Component("test.start.handler")
class TestStartHandler implements ChainHandler<ChainContext> {

    @Override
    public ChainNodeResult handle(ChainContext context, ChainHandlerDefinition handlerDefinition) {
        context.putAttribute("start", true);
        return ChainNodeResult.success(handlerDefinition.handlerName(), 1);
    }
}

@Component("test.middle.handler")
class TestMiddleHandler implements ChainHandler<ChainContext> {

    @Override
    public ChainNodeResult handle(ChainContext context, ChainHandlerDefinition handlerDefinition) {
        context.putAttribute("middle", true);
        return ChainNodeResult.success(handlerDefinition.handlerName(), 1);
    }
}

@Component("test.finally.handler")
class TestFinallyHandler implements ChainHandler<ChainContext> {

    @Override
    public ChainNodeResult handle(ChainContext context, ChainHandlerDefinition handlerDefinition) {
        context.putAttribute("finally", true);
        return ChainNodeResult.success(handlerDefinition.handlerName(), 1);
    }
}
