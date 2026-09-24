package org.example.study.baseSdk.auth;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证引导规则和默认拒绝所依赖的匹配行为。
 */
class SnapshotAuthRuleMatcherTests {

    @Test
    void bootstrapRulesMatchExactMethodAndPath() {
        SnapshotAuthRuleMatcher matcher = new SnapshotAuthRuleMatcher(Optional::empty);
        matcher.initialize();

        assertThat(matcher.match("POST", "/api/detect/sync"))
                .get().extracting(AuthRule::authMode).isEqualTo(AuthMode.SIGNATURE);
        assertThat(matcher.match("GET", "/api/detect/sync")).isEmpty();
        assertThat(matcher.match("POST", "/unknown")).isEmpty();
        assertThat(matcher.currentVersion()).isEqualTo("bootstrap-v1");
    }
}
