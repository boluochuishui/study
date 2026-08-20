package org.example.study.strategy;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Repository
public class StrategyRepository {

    private final AtomicReference<Map<String, DetectStrategy>> strategyCache = new AtomicReference<>();

    public StrategyRepository() {
        strategyCache.set(defaultStrategies());
    }

    public DetectStrategy getBySceneCode(String sceneCode) {
        DetectStrategy strategy = strategyCache.get().get(sceneCode);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported detect scene: " + sceneCode);
        }
        return strategy;
    }

    public List<String> sceneCodes() {
        return strategyCache.get().keySet().stream().sorted().toList();
    }

    public void reload(Map<String, DetectStrategy> newStrategies) {
        strategyCache.set(Map.copyOf(newStrategies));
    }

    private Map<String, DetectStrategy> defaultStrategies() {
        var commentStrategy = new DetectStrategy(
                "comment",
                "v1",
                List.of(
                        new DetectNodeConfig("param_validate", "param_validate", 10, Map.of()),
                        new DetectNodeConfig("keyword_risk", "keyword", 20, Map.of("keywords", "赌博,诈骗,涉黄,暴恐")),
                        new DetectNodeConfig("phone_regex", "regex", 30, Map.of("pattern", "1[3-9]\\d{9}")),
                        new DetectNodeConfig("text_model", "mock_text_model", 40, Map.of("reviewLength", "80"))
                )
        );

        return Map.of(commentStrategy.sceneCode(), commentStrategy);
    }
}
 