package org.example.study.baseSdk.rule.spel;

import java.util.List;

/**
 * 数据库存储和服务间传递的规则发布快照，仅包含可移植的表达式源码。
 */
public record SpelRuleReleaseSnapshot(
        String ruleSetCode,
        String scene,
        long releaseVersion,
        SpelRuleEvaluationMode evaluationMode,
        SpelRuleErrorPolicy errorPolicy,
        boolean enabled,
        List<SpelRuleDefinition> rules
) {

    public SpelRuleReleaseSnapshot {
        rules = List.copyOf(rules);
    }
}
