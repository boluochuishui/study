package org.example.study.baseSdk.chain.support;

import org.example.study.baseSdk.chain.ChainContext;
import org.example.study.baseSdk.chain.ChainExecuteMode;
import org.example.study.baseSdk.chain.ChainExecuteResult;
import org.example.study.baseSdk.chain.ChainRuntime;
import org.example.study.baseSdk.chain.ChainSubmitResult;
import org.springframework.stereotype.Component;

/**
 * Default runtime facade used by business services to execute configured chains.
 */
@Component
public class DefaultChainRuntime implements ChainRuntime {

    private final InMemoryChainDefinitionRepository definitionRepository;
    private final SyncChainExecutor syncChainExecutor;

    public DefaultChainRuntime(InMemoryChainDefinitionRepository definitionRepository, SyncChainExecutor syncChainExecutor) {
        this.definitionRepository = definitionRepository;
        this.syncChainExecutor = syncChainExecutor;
    }

    @Override
    public <C extends ChainContext> ChainExecuteResult execute(String chainName, C context) {
        var definition = definitionRepository.getRequired(chainName);
        if (definition.executeMode() != ChainExecuteMode.SYNC) {
            throw new IllegalArgumentException("Chain does not support sync execution: " + chainName);
        }
        context.setLogContext(context.logContext().withChain(definition.chainName(), definition.version()));
        return syncChainExecutor.execute(context, definition);
    }

    @Override
    public <C extends ChainContext> ChainSubmitResult submit(String chainName, C context) {
        return new ChainSubmitResult(context.taskId(), chainName, "ASYNC_NOT_IMPLEMENTED");
    }
}
