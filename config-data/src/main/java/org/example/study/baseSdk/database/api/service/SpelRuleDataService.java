package org.example.study.baseSdk.database.api.service;

import java.util.List;
import java.util.Optional;

import org.example.study.baseSdk.database.api.model.SaveSpelRuleDraftCommand;
import org.example.study.baseSdk.database.api.model.SaveSpelRuleSetCommand;
import org.example.study.baseSdk.database.api.model.SpelRuleDraftData;
import org.example.study.baseSdk.database.api.model.SpelRuleReleaseData;
import org.example.study.baseSdk.database.api.model.SpelRuleSetData;

/** SpEL 规则持久化边界，不向调用方暴露 MyBatis 类型。 */
public interface SpelRuleDataService {

    List<SpelRuleSetData> listRuleSets(long tenantId);

    Optional<SpelRuleSetData> findRuleSet(long tenantId, String ruleSetCode);

    SpelRuleSetData lockRuleSet(long tenantId, String ruleSetCode);

    SpelRuleSetData saveRuleSet(SaveSpelRuleSetCommand command, long expectedVersion);

    List<SpelRuleDraftData> listDrafts(long tenantId, String ruleSetCode);

    SpelRuleDraftData saveDraft(long tenantId, String ruleSetCode, SaveSpelRuleDraftCommand command,
                                long expectedVersion);

    SpelRuleReleaseData publish(long tenantId, String ruleSetCode, long expectedSetVersion, long releaseVersion,
                                String snapshotJson, String checksum, boolean deleted, String publishedBy);

    List<SpelRuleReleaseData> listReleases(long tenantId, String ruleSetCode);

    Optional<SpelRuleReleaseData> findRelease(long tenantId, String ruleSetCode, long releaseVersion);

    List<SpelRuleReleaseData> listLatestReleases(long tenantId, String scene);

    List<SpelRuleReleaseData> listReleaseChanges(long tenantId, String scene, long afterCursor, int limit);

    long maxReleaseCursor(long tenantId, String scene);
}
