package org.example.study.baseSdk.auth;

import org.example.study.baseSdk.log.LogRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** 验证独立 Web 服务引用 SDK 时鉴权的显式启用与默认日志装配。 */
class AuthAutoConfigurationTests {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withUserConfiguration(AuthAutoConfiguration.class)
            .withBean(AuthConfigSource.class, () -> Optional::empty)
            .withBean(AuthConfigSnapshot.class, () -> new AuthConfigSnapshot("initial", Instant.EPOCH,
                    "system", List.of(new AuthRule("sample", "sample", "/sample", Set.of("GET"),
                    AuthMode.ANONYMOUS, Map.of(), 1, true)), "initial"))
            .withBean(ObjectMapper.class, () -> mock(ObjectMapper.class));

    @Test
    void disabledByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(AuthenticationFilter.class));
    }

    @Test
    void enabledSdkCreatesWebFilterAndFallbackLogRecorder() {
        runner.withPropertyValues("study.auth.enabled=true", "study.auth.app-id=demo-app",
                        "study.auth.app-secret=demo-secret")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthenticationFilter.class);
                    assertThat(context).hasSingleBean(LogRecorder.class);
                    assertThat(context.getBean(AuthRuleMatcher.class).match("GET", "/sample")).isPresent();
                });
    }
}
