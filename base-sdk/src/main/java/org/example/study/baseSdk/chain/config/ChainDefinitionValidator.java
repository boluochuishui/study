package org.example.study.baseSdk.chain.config;

import org.example.study.baseSdk.chain.ChainDefinition;

import java.util.List;

/**
 * Validates chain definitions before they are exposed to runtime execution.
 */
public interface ChainDefinitionValidator {

    void validate(List<ChainDefinition> definitions);
}
