package org.example.study.baseSdk.auth;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证引导规则和默认拒绝所依赖的匹配行为。
 */
class SnapshotAuthRuleMatcherTests {

    @Test
    void bootstrapRulesMatchExactMethodAndPath() {
        AuthConfigSnapshot bootstrap = new AuthConfigSnapshot("bootstrap-v1", Instant.EPOCH, "system",
                List.of(new AuthRule("detect-sync", "同步检测", "/api/detect/sync", Set.of("POST"),
                        AuthMode.SIGNATURE, Map.of(), 10, true)), "bootstrap");
        SnapshotAuthRuleMatcher matcher = new SnapshotAuthRuleMatcher(Optional::empty, bootstrap);
        matcher.initialize();

        assertThat(matcher.match("POST", "/api/detect/sync"))
                .get().extracting(AuthRule::authMode).isEqualTo(AuthMode.SIGNATURE);
        assertThat(matcher.match("GET", "/api/detect/sync")).isEmpty();
        assertThat(matcher.match("POST", "/unknown")).isEmpty();
        assertThat(matcher.currentVersion()).isEqualTo("bootstrap-v1");
    }
}
