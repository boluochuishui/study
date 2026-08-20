package org.example.study.baseSdk.chain.config;

import org.example.study.baseSdk.chain.ChainDefinition;

import java.util.List;

public interface ChainDefinitionValidator {

    void validate(List<ChainDefinition> definitions);
}
