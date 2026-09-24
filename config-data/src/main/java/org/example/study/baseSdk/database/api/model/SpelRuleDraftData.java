package org.example.study.baseSdk.database.api.model;

import java.time.LocalDateTime;

/** 管理台维护的单条 SpEL 规则草稿。 */
public record SpelRuleDraftData(Long id, Long tenantId, Long ruleSetId, String ruleId, String ruleName,
                                String expression, int priority, String attributesJson, Long version,
                                boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
