package org.example.study.baseSdk.chain.config;

import org.apache.commons.lang3.StringUtils;
import org.example.study.baseSdk.chain.ChainDefinition;
import org.example.study.baseSdk.chain.ChainExecuteMode;
import org.example.study.baseSdk.chain.ChainExceptionPolicy;
import org.example.study.baseSdk.chain.ChainHandlerDefinition;
import org.example.study.baseSdk.chain.HandlerReference;
import org.example.study.baseSdk.constant.BaseChars;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 从 *.chain.properties 配置文件组装责任链定义。
 */
@Component
public class PropertiesChainDefinitionLoader implements ChainDefinitionLoader {

    private final ChainPropertiesResourceLoader resourceLoader;
    private final HandlerReferenceResolver referenceResolver;

    public PropertiesChainDefinitionLoader(ChainPropertiesResourceLoader resourceLoader, HandlerReferenceResolver referenceResolver) {
        this.resourceLoader = resourceLoader;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public List<ChainDefinition> load() {
        return resourceLoader.load().stream().map(this::loadOne).toList();
    }

    private ChainDefinition loadOne(Resource resource) {
        Properties properties = new Properties();
        try (var inputStream = resource.getInputStream()) {
            properties.load(inputStream);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load chain properties: " + resource.getDescription(), ex);
        }
        return new ChainDefinition(
                required(properties, "chain.name"),
                properties.getProperty("chain.version", "v1").trim(),
                ChainExecuteMode.valueOf(properties.getProperty("chain.execute.mode", "SYNC").trim()),
                ChainExceptionPolicy.valueOf(properties.getProperty("chain.exception.policy", "BREAK").trim()),
                optionalReference(properties.getProperty("timeout.exception.handler")),
                parseHandlers(required(properties, "execute.handlers")),
                optionalHandler(properties.getProperty("finally.handler"))
        );
    }

    private List<ChainHandlerDefinition> parseHandlers(String rawHandlers) {
        return Arrays.stream(rawHandlers.split(BaseChars.COMMA))
                .map(String::trim)
                .filter(reference -> !reference.isEmpty())
                .map(this::toHandlerDefinition)
                .toList();
    }

    private ChainHandlerDefinition optionalHandler(String rawReference) {
        if (StringUtils.isBlank(rawReference)) {
            return null;
        }
        return toHandlerDefinition(rawReference);
    }

    private ChainHandlerDefinition toHandlerDefinition(String rawReference) {
        HandlerReference reference = referenceResolver.parse(rawReference);
        return new ChainHandlerDefinition(reference.name(), reference, referenceResolver.resolve(reference), Map.of());
    }

    private HandlerReference optionalReference(String rawReference) {
        if (StringUtils.isBlank(rawReference)) {
            return null;
        }
        return referenceResolver.parse(rawReference);
    }

    private String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Missing required chain property: " + key);
        }
        return value.trim();
    }
}
