package org.example.study.admin.api;

/** 规则集编译校验结果。 */
public record RuleValidationResult(String ruleSetCode, int compiledRuleCount, boolean valid) {
}
