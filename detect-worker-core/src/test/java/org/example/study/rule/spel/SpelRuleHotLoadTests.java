package org.example.study.rule.spel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.example.study.baseSdk.database.api.model.SpelRuleReleaseData;
import org.example.study.baseSdk.database.api.service.SpelRuleDataService;
import org.example.study.baseSdk.hotload.ChangeOperation;
import org.example.study.baseSdk.rule.spel.SpelRuleDefinition;
import org.example.study.baseSdk.rule.spel.SpelRuleEngine;
import org.example.study.baseSdk.rule.spel.SpelRuleErrorPolicy;
import org.example.study.baseSdk.rule.spel.SpelRuleEvaluationMode;
import org.example.study.baseSdk.rule.spel.SpelRuleReleaseSnapshot;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** 验证数据库发布快照到内存规则目录的转换。 */
class SpelRuleHotLoadTests {

    private final SpelRuleDataService dataService = mock(SpelRuleDataService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SpelRuleReleaseSource source = new SpelRuleReleaseSource(dataService, objectMapper, 7, "TEXT");

    @Test
    void fullLoadUsesLatestSceneCursorAndCompilesCatalog() throws Exception {
        SpelRuleReleaseData release = release(7, 2, snapshot(2, "#score >= 80"));
        when(dataService.maxReleaseCursor(7, "TEXT")).thenReturn(7L);
        when(dataService.listLatestReleases(7, "TEXT")).thenReturn(List.of(release));

        var full = source.loadFull();
        SpelRuleCatalog catalog = new SpelRuleHotLoadModule(source, new SpelRuleEngine()).compile(full.items());
        var evaluation = catalog.getRequired("text.default").ruleSet().evaluate(null, Map.of("score", 90),
                SpelRuleEvaluationMode.ALL_MATCHES, SpelRuleErrorPolicy.FAIL_FAST);

        assertThat(full.cursor()).isEqualTo(7);
        assertThat(evaluation.matched()).isTrue();
        assertThat(catalog.getRequired("text.default").releaseVersion()).isEqualTo(2);
    }

    @Test
    void incrementalDisabledReleaseBecomesDelete() {
        SpelRuleReleaseData deleted = new SpelRuleReleaseData(8, 7, "text.default", 3, "TEXT", "{}",
                "unused", true, "admin", LocalDateTime.now());
        when(dataService.listReleaseChanges(7, "TEXT", 7, 100)).thenReturn(List.of(deleted));

        var changes = source.loadChanges(7, 100);

        assertThat(changes).singleElement().satisfies(change -> {
            assertThat(change.cursor()).isEqualTo(8);
            assertThat(change.operation()).isEqualTo(ChangeOperation.DELETE);
            assertThat(change.key()).isEqualTo("text.default");
        });
    }

    @Test
    void checksumMismatchRejectsCandidateSnapshot() throws Exception {
        SpelRuleReleaseData release = release(7, 2, snapshot(2, "true"));
        SpelRuleReleaseData corrupted = new SpelRuleReleaseData(release.releaseId(), release.tenantId(),
                release.ruleSetCode(),
                release.releaseVersion(), release.scene(), release.snapshotJson(), "invalid", false,
                release.publishedBy(), release.publishedAt());
        when(dataService.maxReleaseCursor(7, "TEXT")).thenReturn(7L);
        when(dataService.listLatestReleases(7, "TEXT")).thenReturn(List.of(corrupted));

        assertThatThrownBy(source::loadFull)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SpEL rule release checksum does not match: 7");
    }

    private SpelRuleReleaseSnapshot snapshot(long version, String expression) {
        return new SpelRuleReleaseSnapshot("text.default", "TEXT", version,
                SpelRuleEvaluationMode.ALL_MATCHES, SpelRuleErrorPolicy.SKIP_FAILED, true,
                List.of(new SpelRuleDefinition("score", expression, 10, true, Map.of("action", "REVIEW"))));
    }

    private SpelRuleReleaseData release(long releaseId, long version, SpelRuleReleaseSnapshot snapshot)
            throws Exception {
        String json = objectMapper.writeValueAsString(snapshot);
        String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(json.getBytes(StandardCharsets.UTF_8)));
        return new SpelRuleReleaseData(releaseId, 7, snapshot.ruleSetCode(), version, snapshot.scene(), json,
                checksum, false, "admin", LocalDateTime.now());
    }
}
