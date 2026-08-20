package org.example.study.strategy;

import java.util.List;

public record DetectStrategy(
        String sceneCode,
        String version,
        List<DetectNodeConfig> nodes
) {
}
