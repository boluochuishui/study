package org.example.study.baseSdk.auth;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 使用原子引用维护预编译规则，刷新失败时继续使用上一有效快照。
 */
@Component
public class SnapshotAuthRuleMatcher implements AuthRuleMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotAuthRuleMatcher.class);
    private final AuthConfigSource configSource;
    private final PathPatternParser parser = new PathPatternParser();
    private final AtomicReference<CompiledSnapshot> current = new AtomicReference<>();

    public SnapshotAuthRuleMatcher(AuthConfigSource configSource) {
        this.configSource = configSource;
    }

    @PostConstruct
    void initialize() {
        current.set(compile(bootstrapSnapshot()));
    }

    @Scheduled(initialDelayString = "${study.auth.refresh-delay-millis:30000}",
            fixedDelayString = "${study.auth.refresh-delay-millis:30000}")
    public void refresh() {
        try {
            configSource.loadLatest().ifPresent(snapshot -> current.set(compile(snapshot)));
        } catch (Exception exception) {
            LOGGER.warn("Authentication snapshot refresh failed; keeping version {}", currentVersion(), exception);
        }
    }

    @Override
    public Optional<AuthRule> match(String method, String path) {
        String normalizedMethod = method.toUpperCase(Locale.ROOT);
        PathContainer requestPath = PathContainer.parsePath(path);
        return current.get().rules().stream()
                .filter(item -> item.rule().methods().isEmpty()
                        || item.rule().methods().contains(normalizedMethod))
                .filter(item -> item.pattern().matches(requestPath))
                .map(CompiledRule::rule)
                .findFirst();
    }

    @Override
    public String currentVersion() {
        CompiledSnapshot snapshot = current.get();
        return snapshot == null ? "uninitialized" : snapshot.version();
    }

    private CompiledSnapshot compile(AuthConfigSnapshot snapshot) {
        if (snapshot == null || snapshot.version() == null || snapshot.version().isBlank()) {
            throw new IllegalArgumentException("Authentication snapshot version must not be blank");
        }
        List<CompiledRule> rules = snapshot.rules().stream()
                .filter(AuthRule::enabled)
                .sorted(Comparator.comparingInt(AuthRule::priority))
                .map(this::compileRule)
                .toList();
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("Authentication snapshot must contain enabled rules");
        }
        return new CompiledSnapshot(snapshot.version(), rules);
    }

    private CompiledRule compileRule(AuthRule rule) {
        if (rule.ruleId() == null || rule.ruleId().isBlank()
                || rule.pathPattern() == null || rule.pathPattern().isBlank()
                || rule.authMode() == null) {
            throw new IllegalArgumentException("Authentication rule is incomplete");
        }
        Set<String> normalizedMethods = rule.methods().stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        AuthRule normalized = new AuthRule(rule.ruleId(), rule.name(), rule.pathPattern(), normalizedMethods,
                rule.authMode(), rule.options(), rule.priority(), true);
        return new CompiledRule(normalized, parser.parse(normalized.pathPattern()));
    }

    private AuthConfigSnapshot bootstrapSnapshot() {
        return new AuthConfigSnapshot(
                "bootstrap-v1",
                Instant.EPOCH,
                "system",
                List.of(
                        new AuthRule("detect-sync", "同步检测", "/api/detect/sync", Set.of("POST"),
                                AuthMode.SIGNATURE, java.util.Map.of("requiredPermission", "detect:sync"), 10, true),
                        new AuthRule("detect-async", "异步检测", "/api/detect/async", Set.of("POST"),
                                AuthMode.SIGNATURE, java.util.Map.of("requiredPermission", "detect:async"), 20, true),
                        new AuthRule("health-liveness", "存活检查", "/actuator/health/liveness", Set.of("GET"),
                                AuthMode.ANONYMOUS, java.util.Map.of(), 100, true),
                        new AuthRule("health-readiness", "就绪检查", "/actuator/health/readiness", Set.of("GET"),
                                AuthMode.ANONYMOUS, java.util.Map.of(), 110, true)
                ),
                "bootstrap"
        );
    }

    private record CompiledSnapshot(String version, List<CompiledRule> rules) {
    }

    private record CompiledRule(AuthRule rule, PathPattern pattern) {
    }
}
