package org.example.study.baseSdk.rule.spel;

/**
 * 跳过失败规则时返回的结构化错误，不包含待检测数据。
 */
public record SpelRuleError(String ruleId, String errorType, String message) {
}
