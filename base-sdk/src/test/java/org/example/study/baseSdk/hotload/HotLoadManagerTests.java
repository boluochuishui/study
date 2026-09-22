package org.example.study.baseSdk.hotload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

/**
 * 验证全量、增量及失败回退的快照语义。
 */
class HotLoadManagerTests {

    @Test
    void startupFullLoadAndIncrementalUpsertDelete() {
        FakeSource source = new FakeSource();
        HotLoadManager manager = manager(source, new HotLoadProperties());

        manager.loadAtStartup();
        Map<String, String> first = manager.getRequired("sample", Map.class);
        assertThat(first).containsExactlyInAnyOrderEntriesOf(Map.of("a", "one", "b", "two"));
        assertThat(manager.status("sample").cursor()).isEqualTo(10);

        source.changes = List.of(
                new IncrementalChange<>(11, ChangeOperation.UPSERT, "a", "updated"),
                new IncrementalChange<>(12, ChangeOperation.DELETE, "b", null));
        manager.refreshAll();

        assertThat(manager.getRequired("sample", Map.class)).containsExactlyInAnyOrderEntriesOf(Map.of("a", "updated"));
        assertThat(first).containsExactlyInAnyOrderEntriesOf(Map.of("a", "one", "b", "two"));
        assertThat(manager.status("sample").cursor()).isEqualTo(12);
    }

    @Test
    void failedCompilationKeepsPreviousSnapshotAndCursor() {
        FakeSource source = new FakeSource();
        AtomicBoolean failCompilation = new AtomicBoolean();
        HotLoadModule<String, String, Map<String, String>> module = module(source, failCompilation);
        HotLoadManager manager = new HotLoadManager(List.of(module), new HotLoadProperties());
        manager.loadAtStartup();
        failCompilation.set(true);
        source.changes = List.of(new IncrementalChange<>(11, ChangeOperation.UPSERT, "a", "broken"));

        manager.refreshAll();

        assertThat(manager.getRequired("sample", Map.class)).containsEntry("a", "one");
        assertThat(manager.status("sample").cursor()).isEqualTo(10);
        assertThat(manager.status("sample").lastError()).contains("Invalid compiled configuration");
        failCompilation.set(false);
        manager.refreshAll();
        assertThat(manager.getRequired("sample", Map.class)).containsEntry("a", "broken");
    }

    @Test
    void rejectsOutOfOrderChangesWithoutSwitchingSnapshot() {
        FakeSource source = new FakeSource();
        HotLoadManager manager = manager(source, new HotLoadProperties());
        manager.loadAtStartup();
        source.changes = List.of(
                new IncrementalChange<>(12, ChangeOperation.UPSERT, "a", "new"),
                new IncrementalChange<>(11, ChangeOperation.DELETE, "b", null));

        manager.refreshAll();

        assertThat(manager.getRequired("sample", Map.class)).containsEntry("a", "one").containsEntry("b", "two");
        assertThat(manager.status("sample").cursor()).isEqualTo(10);
    }

    @Test
    void periodicFullLoadReconcilesMissedChanges() {
        FakeSource source = new FakeSource();
        HotLoadProperties properties = new HotLoadProperties();
        properties.setFullRefreshIntervalMillis(0);
        HotLoadManager manager = manager(source, properties);
        manager.loadAtStartup();
        source.full = new FullLoadResult<>(Map.of("recovered", "latest"), 15);

        manager.refreshAll();

        assertThat(manager.getRequired("sample", Map.class)).containsOnlyKeys("recovered");
        assertThat(manager.status("sample").cursor()).isEqualTo(15);
    }

    @Test
    void failedInitialLoadRemainsUnavailableAndCanRetry() {
        FakeSource source = new FakeSource();
        source.failFull = true;
        HotLoadManager manager = manager(source, new HotLoadProperties());

        manager.loadAtStartup();
        assertThat(manager.status("sample").initialized()).isFalse();
        assertThatThrownBy(() -> manager.getRequired("sample", Map.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Hot load module is not initialized: sample");

        source.failFull = false;
        manager.refreshAll();
        assertThat(manager.status("sample").initialized()).isTrue();
    }

    @Test
    void rejectsDuplicateModuleNames() {
        FakeSource source = new FakeSource();
        HotLoadModule<String, String, Map<String, String>> module = module(source, new AtomicBoolean());

        assertThatThrownBy(() -> new HotLoadManager(List.of(module, module), new HotLoadProperties()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicated hot load module: sample");
    }

    private HotLoadManager manager(FakeSource source, HotLoadProperties properties) {
        return new HotLoadManager(List.of(module(source, new AtomicBoolean())), properties);
    }

    private HotLoadModule<String, String, Map<String, String>> module(FakeSource source, AtomicBoolean failCompilation) {
        return new HotLoadModule<>() {
            @Override
            public String name() {
                return "sample";
            }

            @Override
            public HotLoadSource<String, String> source() {
                return source;
            }

            @Override
            public Map<String, String> compile(Map<String, String> items) {
                if (failCompilation.get()) {
                    throw new IllegalArgumentException("Invalid compiled configuration");
                }
                return Map.copyOf(items);
            }
        };
    }

    private static final class FakeSource implements HotLoadSource<String, String> {

        private FullLoadResult<String, String> full = new FullLoadResult<>(Map.of("a", "one", "b", "two"), 10);
        private List<IncrementalChange<String, String>> changes = List.of();
        private boolean failFull;

        @Override
        public FullLoadResult<String, String> loadFull() {
            if (failFull) {
                throw new IllegalStateException("Database is unavailable");
            }
            return full;
        }

        @Override
        public List<IncrementalChange<String, String>> loadChanges(long afterCursor, int limit) {
            return changes;
        }
    }
}
