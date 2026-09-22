package org.example.study.baseSdk.chain.config;

import org.example.study.baseSdk.chain.ChainDefinition;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Performs startup-time validation for chain names and resolved handlers.
 */
@Component
public class DefaultChainDefinitionValidator implements ChainDefinitionValidator {

    @Override
    public void validate(List<ChainDefinition> definitions) {
        Set<String> chainNames = new HashSet<>();
        for (ChainDefinition definition : definitions) {
            if (definition.chainName() == null || definition.chainName().isBlank()) {
                throw new IllegalArgumentException("Chain name must not be blank");
            }
            if (!chainNames.add(definition.chainName())) {
                throw new IllegalArgumentException("Duplicated chain name: " + definition.chainName());
            }
            if (definition.executeHandlers() == null || definition.executeHandlers().isEmpty()) {
                throw new IllegalArgumentException("Chain execute.handlers must not be empty: " + definition.chainName());
            }
            definition.executeHandlers().forEach(handlerDefinition -> {
                if (handlerDefinition.handler() == null) {
                    throw new IllegalArgumentException("Chain handler must be resolved: " + handlerDefinition.handlerName());
                }
            });
        }
    }
}
