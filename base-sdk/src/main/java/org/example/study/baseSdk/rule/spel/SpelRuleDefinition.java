package org.example.study.baseSdk.rule.spel;

import java.util.Map;

/**
 * 可由配置中心或数据库承载的通用 SpEL 规则定义。
 */
public record SpelRuleDefinition(String ruleId, String expression, int priority, boolean enabled,
                                 Map<String, String> attributes) {

    public SpelRuleDefinition {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
