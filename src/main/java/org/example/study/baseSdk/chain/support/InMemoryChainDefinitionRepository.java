package org.example.study.baseSdk.chain.support;

import org.example.study.baseSdk.chain.ChainDefinition;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Repository
public class InMemoryChainDefinitionRepository {

    private final AtomicReference<Map<String, ChainDefinition>> chainCache = new AtomicReference<>(Map.of());

    public ChainDefinition getRequired(String chainName) {
        ChainDefinition definition = chainCache.get().get(chainName);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported chain name: " + chainName);
        }
        return definition;
    }

    public List<ChainDefinition> list() {
        return chainCache.get().values().stream().toList();
    }

    public void initialize(List<ChainDefinition> definitions) {
        chainCache.set(definitions.stream()
                .collect(Collectors.toUnmodifiableMap(ChainDefinition::chainName, definition -> definition)));
    }
}
