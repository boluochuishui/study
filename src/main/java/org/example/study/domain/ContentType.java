package org.example.study.domain;

import java.util.Arrays;

public enum ContentType {
    TEXT,
    IMAGE,
    AUDIO,
    VIDEO,
    MULTIMODAL;

    public static ContentType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported content type: " + value));
    }
}
