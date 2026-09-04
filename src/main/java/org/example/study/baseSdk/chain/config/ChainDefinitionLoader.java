package org.example.study.baseSdk.chain.config;

import org.example.study.baseSdk.chain.ChainDefinition;

import java.util.List;

/**
 * Loads immutable chain definitions from a concrete configuration source.
 */
public interface ChainDefinitionLoader {

    List<ChainDefinition> load();
}
