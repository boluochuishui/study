package org.example.study.baseSdk.chain.spring;

import jakarta.annotation.PostConstruct;
import org.example.study.baseSdk.chain.config.ChainDefinitionLoader;
import org.example.study.baseSdk.chain.config.ChainDefinitionValidator;
import org.example.study.baseSdk.chain.support.InMemoryChainDefinitionRepository;
import org.springframework.stereotype.Component;

@Component
public class ChainRuntimeInitializer {

    private final ChainDefinitionLoader definitionLoader;
    private final ChainDefinitionValidator definitionValidator;
    private final InMemoryChainDefinitionRepository definitionRepository;

    public ChainRuntimeInitializer(
            ChainDefinitionLoader definitionLoader,
            ChainDefinitionValidator definitionValidator,
            InMemoryChainDefinitionRepository definitionRepository
    ) {
        this.definitionLoader = definitionLoader;
        this.definitionValidator = definitionValidator;
        this.definitionRepository = definitionRepository;
    }

    @PostConstruct
    public void initialize() {
        var definitions = definitionLoader.load();
        definitionValidator.validate(definitions);
        definitionRepository.initialize(definitions);
    }
}
