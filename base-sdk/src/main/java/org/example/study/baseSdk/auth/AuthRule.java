package org.example.study.baseSdk.auth;

import java.util.Map;
import java.util.Set;

/**
 * URL 鉴权规则，数值更小的规则拥有更高优先级。
 */
public record AuthRule(
        String ruleId,
        String name,
        String pathPattern,
        Set<String> methods,
        AuthMode authMode,
        Map<String, String> options,
        int priority,
        boolean enabled
) {
    public AuthRule {
        methods = methods == null ? Set.of() : Set.copyOf(methods);
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
