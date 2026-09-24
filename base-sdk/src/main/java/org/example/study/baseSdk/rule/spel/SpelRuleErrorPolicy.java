package org.example.study.baseSdk.rule.spel;

/**
 * 单条规则执行失败时的处理策略。
 */
public enum SpelRuleErrorPolicy {
    FAIL_FAST,
    SKIP_FAILED
}
