package org.example.study.baseSdk.chain.support;

import org.example.study.baseSdk.chain.ChainContext;
import org.example.study.baseSdk.chain.ChainDefinition;
import org.example.study.baseSdk.chain.ChainExecuteResult;
import org.example.study.baseSdk.chain.ChainExceptionHandler;
import org.example.study.baseSdk.chain.ChainExecutor;
import org.example.study.baseSdk.chain.ChainExecutionListener;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.ChainNodeResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Sequential chain executor for startup-assembled responsibility chains.
 */
@Component
public class SyncChainExecutor implements ChainExecutor<ChainContext> {

    private final ChainExceptionHandler<ChainContext> exceptionHandler;
    private final ChainExecutionListener<ChainContext> executionListener;

    public SyncChainExecutor(ChainExceptionHandler<ChainContext> exceptionHandler, ChainExecutionListener<ChainContext> executionListener) {
        this.exceptionHandler = exceptionHandler;
        this.executionListener = executionListener;
    }

    @Override
    public ChainExecuteResult execute(ChainContext context, ChainDefinition definition) {
        List<ChainNodeResult> results = new ArrayList<>();
        executionListener.beforeChain(context, definition);
        for (ChainHandlerDefinition handlerDefinition : definition.executeHandlers()) {
            ChainNodeResult result = invokeHandler(context, definition, handlerDefinition, "EXECUTE");
            results.add(result);
            if (result.breakChain()) {
                break;
            }
        }
        if (definition.finallyHandler() != null) {
            results.add(invokeHandler(context, definition, definition.finallyHandler(), "FINALLY"));
        }
        ChainExecuteResult executeResult = ChainExecuteResult.from(context, results, Instant.now());
        executionListener.afterChain(context, definition, executeResult);
        return executeResult;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ChainNodeResult invokeHandler(ChainContext context, ChainDefinition definition, ChainHandlerDefinition handlerDefinition, String role) {
        executionListener.beforeHandler(context, definition, handlerDefinition, role);
        try {
            ChainNodeResult result = handlerDefinition.handler().handle(context, handlerDefinition).withHandlerRole(role);
            executionListener.afterHandler(context, definition, handlerDefinition, result);
            return result;
        } catch (Exception ex) {
            executionListener.onHandlerException(context, definition, handlerDefinition, ex);
            ChainNodeResult result = exceptionHandler.handle(context, definition, handlerDefinition, ex).withHandlerRole(role);
            executionListener.afterHandler(context, definition, handlerDefinition, result);
            return result;
        }
    }
}
