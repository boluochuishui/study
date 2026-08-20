package org.example.study.baseSdk.chain;

import java.util.Map;

public record ChainNodeConfig(
        String code,
        String name,
        String type,
        int order,
        boolean enabled,
        Map<String, String> properties
) {

    public String property(String key, String defaultValue) {
        return properties == null ? defaultValue : properties.getOrDefault(key, defaultValue);
    }
}
