package org.example.study.baseSdk.rule.spel;

import java.util.Map;

/**
 * 已命中的规则及其业务扩展属性。
 */
public record SpelRuleMatch(String ruleId, int priority, Map<String, String> attributes) {

    public SpelRuleMatch {
        attributes = Map.copyOf(attributes);
    }
}
