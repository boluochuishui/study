package org.example.study.baseSdk.database.api.model;

import java.time.LocalDateTime;

/** 已发布且不可修改的 SpEL 规则集快照。 */
public record SpelRuleReleaseData(long releaseId, long tenantId, String ruleSetCode, long releaseVersion,
                                  String scene, String snapshotJson, String checksum, boolean deleted,
                                  String publishedBy, LocalDateTime publishedAt) {
}
