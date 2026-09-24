package org.example.study.baseSdk.database.api.model;

import java.time.LocalDateTime;

/** SpEL 规则集元数据。 */
public record SpelRuleSetData(Long id, Long tenantId, String ruleSetCode, String ruleSetName, String scene,
                              String evaluationMode, String errorPolicy, long publishedVersion,
                              Long version, boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
