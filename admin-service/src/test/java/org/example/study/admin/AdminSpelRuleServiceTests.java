package org.example.study.admin.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.example.study.admin.api.AdminApiException;
import org.example.study.admin.api.PublishRuleSetRequest;
import org.example.study.admin.service.AdminSpelRuleService;
import org.example.study.baseSdk.database.api.model.SpelRuleDraftData;
import org.example.study.baseSdk.database.api.model.SpelRuleReleaseData;
import org.example.study.baseSdk.database.api.model.SpelRuleSetData;
import org.example.study.baseSdk.database.api.service.SpelRuleDataService;
import org.example.study.baseSdk.rule.spel.SpelRuleEngine;
import org.example.study.baseSdk.rule.spel.SpelRuleReleaseSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

/** 验证管理台发布校验和不可变快照生成。 */
class AdminSpelRuleServiceTests {

    private final SpelRuleDataService dataService = mock(SpelRuleDataService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AdminSpelRuleService service = new AdminSpelRuleService(
            dataService, new SpelRuleEngine(), objectMapper);

    @BeforeEach
    void setIdentity() {
        AdminSecurityContext.set(new AdminPrincipal(9, 7, "tenant-a", "real-admin",
                "Real Admin", "hash", java.util.Set.of(AdminPermissions.RULE_PUBLISH)));
    }

    @AfterEach
    void clearIdentity() {
        AdminSecurityContext.clear();
    }

    @Test
    void publishCompilesDraftsAndStoresExpressionSource() throws Exception {
        SpelRuleSetData ruleSet = ruleSet(3, 2);
        when(dataService.lockRuleSet(7, "text.default")).thenReturn(ruleSet);
        when(dataService.listDrafts(7, "text.default")).thenReturn(List.of(draft("#score >= 80")));
        when(dataService.publish(anyLong(), anyString(), anyLong(), anyLong(), anyString(), anyString(),
                anyBoolean(), anyString())).thenReturn(release(10, 3, "{}"));

        SpelRuleReleaseData result = service.publish("text.default", new PublishRuleSetRequest(3L, "admin"));

        ArgumentCaptor<String> snapshotCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> checksumCaptor = ArgumentCaptor.forClass(String.class);
        verify(dataService).publish(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq("text.default"),
                org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq(3L),
                snapshotCaptor.capture(), checksumCaptor.capture(), org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq("real-admin"));
        SpelRuleReleaseSnapshot snapshot = objectMapper.readValue(
                snapshotCaptor.getValue(), SpelRuleReleaseSnapshot.class);
        assertThat(result.releaseVersion()).isEqualTo(3);
        assertThat(snapshot.rules()).singleElement().extracting(rule -> rule.expression()).isEqualTo("#score >= 80");
        assertThat(checksumCaptor.getValue()).hasSize(64);
    }

    @Test
    void dangerousExpressionCannotBePublished() {
        when(dataService.lockRuleSet(7, "text.default")).thenReturn(ruleSet(3, 2));
        when(dataService.listDrafts(7, "text.default"))
                .thenReturn(List.of(draft("T(java.lang.Runtime).getRuntime() != null")));

        assertThatThrownBy(() -> service.publish("text.default", new PublishRuleSetRequest(3L, "admin")))
                .isInstanceOf(AdminApiException.class)
                .satisfies(exception -> assertThat(((AdminApiException) exception).code()).isEqualTo("RULE_INVALID"));
        verify(dataService, never()).publish(anyLong(), anyString(), anyLong(), anyLong(), anyString(), anyString(),
                anyBoolean(), anyString());
    }

    @Test
    void rollbackCreatesANewVersionFromHistoricalSource() throws Exception {
        SpelRuleSetData ruleSet = ruleSet(4, 3);
        SpelRuleReleaseSnapshot old = new SpelRuleReleaseSnapshot("text.default", "TEXT", 1,
                org.example.study.baseSdk.rule.spel.SpelRuleEvaluationMode.ALL_MATCHES,
                org.example.study.baseSdk.rule.spel.SpelRuleErrorPolicy.SKIP_FAILED, true,
                List.of(new org.example.study.baseSdk.rule.spel.SpelRuleDefinition(
                        "score", "#score >= 70", 10, true, java.util.Map.of("action", "REVIEW"))));
        when(dataService.lockRuleSet(7, "text.default")).thenReturn(ruleSet);
        when(dataService.findRelease(7, "text.default", 1))
                .thenReturn(Optional.of(release(5, 1, objectMapper.writeValueAsString(old))));
        when(dataService.publish(anyLong(), anyString(), anyLong(), anyLong(), anyString(), anyString(),
                anyBoolean(), anyString())).thenReturn(release(11, 4, "{}"));

        service.rollback("text.default", 1, new PublishRuleSetRequest(4L, "admin"));

        verify(dataService).publish(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq("text.default"),
                org.mockito.ArgumentMatchers.eq(4L), org.mockito.ArgumentMatchers.eq(4L),
                anyString(), anyString(), org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq("real-admin"));
    }

    private SpelRuleSetData ruleSet(long version, long publishedVersion) {
        return new SpelRuleSetData(1L, 7L, "text.default", "Default text rules", "TEXT",
                "ALL_MATCHES", "SKIP_FAILED", publishedVersion, version, true,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private SpelRuleDraftData draft(String expression) {
        return new SpelRuleDraftData(1L, 7L, 1L, "score", "Score rule", expression, 10,
                "{\"action\":\"REVIEW\"}", 1L, true, LocalDateTime.now(), LocalDateTime.now());
    }

    private SpelRuleReleaseData release(long releaseId, long releaseVersion, String snapshot) {
        return new SpelRuleReleaseData(releaseId, 7, "text.default", releaseVersion, "TEXT", snapshot,
                "checksum", false, "admin", LocalDateTime.now());
    }
}
