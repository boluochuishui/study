package org.example.study.baseSdk.chain;

import java.util.Map;

/**
 * Runtime definition of a configured handler after its Bean reference is resolved.
 */
public record ChainHandlerDefinition(
        String handlerName,
        HandlerReference reference,
        ChainHandler handler,
        Map<String, String> properties
) {

    public ChainHandlerDefinition {
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    public String property(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }
}
