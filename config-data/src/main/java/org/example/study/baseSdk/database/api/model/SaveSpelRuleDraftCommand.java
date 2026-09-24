package org.example.study.baseSdk.database.api.model;

/** 新建或更新 SpEL 规则草稿的数据库命令。 */
public record SaveSpelRuleDraftCommand(String ruleId, String ruleName, String expression,
                                       int priority, String attributesJson, boolean enabled) {
}
