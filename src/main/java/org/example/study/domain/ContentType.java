package org.example.study.domain;

import java.util.Arrays;

/**
 * Supported moderation content categories.
 */
public enum ContentType {
    TEXT("text.detect.chain"),
    IMAGE("image.detect.chain"),
    AUDIO("audio.detect.chain"),
    VIDEO("video.detect.chain"),
    MULTIMODAL("multimodal.detect.chain");

    private final String chainName;

    ContentType(String chainName) {
        this.chainName = chainName;
    }

    public String chainName() {
        return chainName;
    }

    public static ContentType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported content type: " + value));
    }
}
