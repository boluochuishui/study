package org.example.study.baseSdk.chain.config;

import org.example.study.baseSdk.chain.ChainDefinition;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DefaultChainDefinitionValidator implements ChainDefinitionValidator {

    @Override
    public void validate(List<ChainDefinition> definitions) {
        Set<String> chainNames = new HashSet<>();
        for (ChainDefinition definition : definitions) {
            validateOne(definition, chainNames);
        }
    }

    private void validateOne(ChainDefinition definition, Set<String> chainNames) {
        if (definition.chainName() == null || definition.chainName().isBlank()) {
            throw new IllegalArgumentException("Chain name must not be blank");
        }
        if (!chainNames.add(definition.chainName())) {
            throw new IllegalArgumentException("Duplicated chain name: " + definition.chainName());
        }
        if (definition.executeHandlers() == null || definition.executeHandlers().isEmpty()) {
            throw new IllegalArgumentException("Chain execute.handlers must not be empty: " + definition.chainName());
        }
        for (var handlerDefinition : definition.executeHandlers()) {
            if (handlerDefinition.handler() == null) {
                throw new IllegalArgumentException("Chain handler must be resolved: " + handlerDefinition.handlerName());
            }
        }
        if (definition.finallyHandler() != null && definition.finallyHandler().handler() == null) {
            throw new IllegalArgumentException("Finally handler must be resolved: " + definition.finallyHandler().handlerName());
        }
    }
}
