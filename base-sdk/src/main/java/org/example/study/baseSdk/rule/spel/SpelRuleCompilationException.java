package org.example.study.baseSdk.rule.spel;

/**
 * 规则定义或表达式无法安全编译时抛出。
 */
public class SpelRuleCompilationException extends IllegalArgumentException {

    public SpelRuleCompilationException(String message) {
        super(message);
    }

    public SpelRuleCompilationException(String message, Throwable cause) {
        super(message, cause);
    }
}
