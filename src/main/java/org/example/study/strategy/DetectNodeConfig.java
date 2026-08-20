package org.example.study.strategy;

import java.util.Map;

public record DetectNodeConfig(
        String code,
        String type,
        int order,
        Map<String, String> properties
) {

    public String property(String name, String defaultValue) {
        return properties.getOrDefault(name, defaultValue);
    }
}
