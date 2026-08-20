package org.example.study.baseSdk.chain.config;

import org.example.study.baseSdk.chain.ChainDefinition;
import org.example.study.baseSdk.chain.ChainExecuteMode;
import org.example.study.baseSdk.chain.ChainExceptionPolicy;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.HandlerReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
public class PropertiesChainDefinitionLoader implements ChainDefinitionLoader {

    private final ChainPropertiesResourceLoader resourceLoader;
    private final HandlerReferenceResolver referenceResolver;

    public PropertiesChainDefinitionLoader(
            ChainPropertiesResourceLoader resourceLoader,
            HandlerReferenceResolver referenceResolver
    ) {
        this.resourceLoader = resourceLoader;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public List<ChainDefinition> load() {
        return resourceLoader.load().stream()
                .map(this::loadOne)
                .toList();
    }

    private ChainDefinition loadOne(Resource resource) {
        Properties properties = new Properties();
        try (var inputStream = resource.getInputStream()) {
            properties.load(inputStream);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load chain properties: " + resource.getDescription(), ex);
        }

        String chainName = required(properties, "chain.name");
        String version = properties.getProperty("chain.version", "v1").trim();
        ChainExecuteMode executeMode = ChainExecuteMode.valueOf(properties.getProperty("chain.execute.mode", "SYNC").trim());
        ChainExceptionPolicy exceptionPolicy = ChainExceptionPolicy.valueOf(properties.getProperty("chain.exception.policy", "BREAK").trim());
        HandlerReference exceptionHandler = optionalReference(properties.getProperty("timeout.exception.handler"));
        List<ChainHandlerDefinition> executeHandlers = parseHandlers(required(properties, "execute.handlers"));
        ChainHandlerDefinition finallyHandler = optionalHandler(properties.getProperty("finally.handler"));

        return new ChainDefinition(
                chainName,
                version,
                executeMode,
                exceptionPolicy,
                exceptionHandler,
                executeHandlers,
                finallyHandler
        );
    }

    private List<ChainHandlerDefinition> parseHandlers(String rawHandlers) {
        return Arrays.stream(rawHandlers.split(","))
                .map(String::trim)
                .filter(reference -> !reference.isEmpty())
                .map(this::toHandlerDefinition)
                .toList();
    }

    private ChainHandlerDefinition optionalHandler(String rawReference) {
        if (rawReference == null || rawReference.isBlank()) {
            return null;
        }
        return toHandlerDefinition(rawReference);
    }

    private ChainHandlerDefinition toHandlerDefinition(String rawReference) {
        HandlerReference reference = referenceResolver.parse(rawReference);
        return new ChainHandlerDefinition(
                reference.name(),
                reference,
                referenceResolver.resolve(reference),
                Map.of()
        );
    }

    private HandlerReference optionalReference(String rawReference) {
        if (rawReference == null || rawReference.isBlank()) {
            return null;
        }
        return referenceResolver.parse(rawReference);
    }

    private String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required chain property: " + key);
        }
        return value.trim();
    }
}
