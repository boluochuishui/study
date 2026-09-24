package org.example.study.baseSdk.database.api.model;

/** 新建或更新 SpEL 规则集的数据库命令。 */
public record SaveSpelRuleSetCommand(long tenantId, String ruleSetCode, String ruleSetName, String scene,
                                     String evaluationMode, String errorPolicy, boolean enabled) {
}
