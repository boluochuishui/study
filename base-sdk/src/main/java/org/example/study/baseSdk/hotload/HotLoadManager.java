package org.example.study.baseSdk.hotload;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时执行全量及增量热加载，并按模块原子切换已编译快照。
 */
@Component
public class HotLoadManager {

    private static final Logger log = LoggerFactory.getLogger(HotLoadManager.class);

    private final Map<String, ModuleState<?, ?, ?>> modules;
    private final long fullRefreshIntervalMillis;
    private final int batchSize;

    public HotLoadManager(List<HotLoadModule<?, ?, ?>> moduleList, HotLoadProperties properties) {
        if (properties.getBatchSize() <= 0 || properties.getFullRefreshIntervalMillis() < 0) {
            throw new IllegalArgumentException("Invalid hot load scheduling configuration");
        }
        this.fullRefreshIntervalMillis = properties.getFullRefreshIntervalMillis();
        this.batchSize = properties.getBatchSize();
        Map<String, ModuleState<?, ?, ?>> registered = new LinkedHashMap<>();
        for (HotLoadModule<?, ?, ?> module : moduleList) {
            String name = Objects.requireNonNull(module.name(), "Hot load module name must not be null").trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Hot load module name must not be blank");
            }
            if (registered.putIfAbsent(name, new ModuleState<>(module)) != null) {
                throw new IllegalArgumentException("Duplicated hot load module: " + name);
            }
        }
        this.modules = Map.copyOf(registered);
    }

    /**
     * 应用就绪后尝试首次全量加载；失败的模块将在下一次轮询重试。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadAtStartup() {
        refreshAll();
    }

    @Scheduled(fixedDelayString = "${base-sdk.hotload.poll-interval-ms:30000}",
            initialDelayString = "${base-sdk.hotload.poll-interval-ms:30000}")
    public void refreshAll() {
        for (ModuleState<?, ?, ?> module : modules.values()) {
            module.refresh(fullRefreshIntervalMillis, batchSize);
        }
    }

    /**
     * 读取指定模块当前已生效的快照，未初始化时明确失败。
     */
    public <R> R getRequired(String moduleName, Class<R> resultType) {
        Object value = requiredState(moduleName).currentValue();
        if (value == null) {
            throw new IllegalStateException("Hot load module is not initialized: " + moduleName);
        }
        return resultType.cast(value);
    }

    public HotLoadStatus status(String moduleName) {
        return requiredState(moduleName).status();
    }

    private ModuleState<?, ?, ?> requiredState(String moduleName) {
        ModuleState<?, ?, ?> state = modules.get(moduleName);
        if (state == null) {
            throw new IllegalArgumentException("Unknown hot load module: " + moduleName);
        }
        return state;
    }

    private static final class ModuleState<K, V, R> {

        private final HotLoadModule<K, V, R> module;
        private volatile LoadedState<K, V, R> current;
        private volatile Instant lastSuccessAt;
        private volatile Instant lastFailureAt;
        private volatile String lastError = "";

        private ModuleState(HotLoadModule<K, V, R> module) {
            this.module = module;
        }

        private synchronized void refresh(long fullRefreshIntervalMillis, int batchSize) {
            try {
                Instant now = Instant.now();
                if (current == null || fullRefreshIntervalMillis == 0
                        || now.toEpochMilli() - current.fullLoadedAt().toEpochMilli()
                        >= fullRefreshIntervalMillis) {
                    loadFull(now);
                } else {
                    loadIncremental(now, batchSize);
                }
                lastError = "";
            } catch (Exception exception) {
                lastFailureAt = Instant.now();
                lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
                log.error("Hot load refresh failed, moduleName={}", module.name(), exception);
            }
        }

        private void loadFull(Instant now) {
            FullLoadResult<K, V> full = Objects.requireNonNull(module.source().loadFull(),
                    "Full load result must not be null");
            R compiled = Objects.requireNonNull(module.compile(full.items()),
                    "Compiled hot load snapshot must not be null");
            LoadedState<K, V, R> previous = current;
            if (previous != null && full.cursor() < previous.cursor()) {
                throw new IllegalStateException("Full load cursor moved backwards");
            }
            current = new LoadedState<>(full.items(), compiled, full.cursor(), now);
            lastSuccessAt = now;
            log.info("Hot load full snapshot activated, moduleName={}, cursor={}, itemCount={}",
                    module.name(), full.cursor(), full.items().size());
        }

        private void loadIncremental(Instant now, int batchSize) {
            LoadedState<K, V, R> previous = current;
            List<IncrementalChange<K, V>> changes = List.copyOf(Objects.requireNonNull(
                    module.source().loadChanges(previous.cursor(), batchSize),
                    "Incremental changes must not be null"));
            if (changes.isEmpty()) {
                return;
            }
            if (changes.size() > batchSize) {
                throw new IllegalStateException("Incremental batch exceeds configured size");
            }
            Map<K, V> nextItems = new HashMap<>(previous.items());
            long nextCursor = previous.cursor();
            for (IncrementalChange<K, V> change : changes) {
                if (change.cursor() <= nextCursor) {
                    throw new IllegalStateException("Incremental changes must have strictly increasing cursors");
                }
                if (change.operation() == ChangeOperation.DELETE) {
                    nextItems.remove(change.key());
                } else {
                    nextItems.put(change.key(), change.value());
                }
                nextCursor = change.cursor();
            }
            Map<K, V> immutableItems = Map.copyOf(nextItems);
            R compiled = Objects.requireNonNull(module.compile(immutableItems),
                    "Compiled hot load snapshot must not be null");
            current = new LoadedState<>(immutableItems, compiled, nextCursor, previous.fullLoadedAt());
            lastSuccessAt = now;
            log.info("Hot load incremental snapshot activated, moduleName={}, cursor={}, changeCount={}",
                    module.name(), nextCursor, changes.size());
        }

        private Object currentValue() {
            LoadedState<K, V, R> snapshot = current;
            return snapshot == null ? null : snapshot.compiled();
        }

        private HotLoadStatus status() {
            LoadedState<K, V, R> snapshot = current;
            return new HotLoadStatus(module.name(), snapshot != null,
                    snapshot == null ? 0 : snapshot.cursor(), lastSuccessAt, lastFailureAt, lastError);
        }
    }

    private record LoadedState<K, V, R>(Map<K, V> items, R compiled, long cursor, Instant fullLoadedAt) {
    }
}
