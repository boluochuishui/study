package org.example.study.baseSdk.hotload;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.example.study.StudyApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 验证 Spring 定时器会自动拉取并切换增量快照。
 */
@SpringBootTest(classes = {StudyApplication.class, ScheduledHotLoadIntegrationTests.ModuleConfiguration.class},
        properties = "base-sdk.hotload.poll-interval-ms=50")
class ScheduledHotLoadIntegrationTests {

    @Autowired
    private HotLoadManager manager;

    @Autowired
    private AtomicBoolean changed;

    @Test
    void scheduledRefreshAppliesIncrementalChange() throws InterruptedException {
        assertThat(manager.getRequired("scheduled-test", Map.class)).containsEntry("key", "before");
        changed.set(true);

        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (manager.status("scheduled-test").cursor() < 2 && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }

        assertThat(manager.status("scheduled-test").cursor()).isEqualTo(2);
        assertThat(manager.getRequired("scheduled-test", Map.class)).containsEntry("key", "after");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ModuleConfiguration {

        @Bean
        AtomicBoolean changed() {
            return new AtomicBoolean();
        }

        @Bean
        HotLoadModule<String, String, Map<String, String>> scheduledTestModule(AtomicBoolean changed) {
            return new HotLoadModule<>() {
                private final HotLoadSource<String, String> source = new HotLoadSource<>() {
                    @Override
                    public FullLoadResult<String, String> loadFull() {
                        return new FullLoadResult<>(Map.of("key", "before"), 1);
                    }

                    @Override
                    public List<IncrementalChange<String, String>> loadChanges(long afterCursor, int limit) {
                        if (!changed.get() || afterCursor >= 2) {
                            return List.of();
                        }
                        return List.of(new IncrementalChange<>(2, ChangeOperation.UPSERT, "key", "after"));
                    }
                };

                @Override
                public String name() {
                    return "scheduled-test";
                }

                @Override
                public HotLoadSource<String, String> source() {
                    return source;
                }

                @Override
                public Map<String, String> compile(Map<String, String> items) {
                    return Map.copyOf(items);
                }
            };
        }
    }
}
