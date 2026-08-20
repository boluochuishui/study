package org.example.study.baseSdk.chain;

import java.util.Map;

public record ChainHandlerDefinition(
        String handlerName,
        HandlerReference reference,
        ChainHandler handler,
        Map<String, String> properties
) {

    public String property(String key, String defaultValue) {
        return properties == null ? defaultValue : properties.getOrDefault(key, defaultValue);
    }
}
