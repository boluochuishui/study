package org.example.study.baseSdk.rule.spel;

/**
 * 规则在失败即停模式下执行失败时抛出。
 */
public class SpelRuleEvaluationException extends RuntimeException {

    private final String ruleId;

    public SpelRuleEvaluationException(String ruleId, String message, Throwable cause) {
        super(message, cause);
        this.ruleId = ruleId;
    }

    public String getRuleId() {
        return ruleId;
    }
}
