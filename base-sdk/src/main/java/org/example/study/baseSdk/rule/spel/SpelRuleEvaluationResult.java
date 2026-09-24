package org.example.study.baseSdk.rule.spel;

import java.util.List;

/**
 * 一次规则集执行的命中和错误汇总。
 */
public record SpelRuleEvaluationResult(List<SpelRuleMatch> matches, List<SpelRuleError> errors) {

    public SpelRuleEvaluationResult {
        matches = List.copyOf(matches);
        errors = List.copyOf(errors);
    }

    public boolean matched() {
        return !matches.isEmpty();
    }
}
