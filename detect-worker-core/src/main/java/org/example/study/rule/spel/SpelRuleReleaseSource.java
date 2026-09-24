package org.example.study.rule.spel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.example.study.baseSdk.database.api.model.SpelRuleReleaseData;
import org.example.study.baseSdk.database.api.service.SpelRuleDataService;
import org.example.study.baseSdk.hotload.ChangeOperation;
import org.example.study.baseSdk.hotload.FullLoadResult;
import org.example.study.baseSdk.hotload.HotLoadSource;
import org.example.study.baseSdk.hotload.IncrementalChange;
import org.example.study.baseSdk.rule.spel.SpelRuleReleaseSnapshot;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** 从发布表按场景读取完整快照和增量版本。 */
public class SpelRuleReleaseSource implements HotLoadSource<String, SpelRuleReleaseSnapshot> {

    private final SpelRuleDataService dataService;
    private final ObjectMapper objectMapper;
    private final long tenantId;
    private final String scene;

    public SpelRuleReleaseSource(SpelRuleDataService dataService, ObjectMapper objectMapper,
                                 long tenantId, String scene) {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("Tenant id must be positive");
        }
        this.dataService = dataService;
        this.objectMapper = objectMapper;
        this.tenantId = tenantId;
        this.scene = scene;
    }

    @Override
    @Transactional(readOnly = true)
    public FullLoadResult<String, SpelRuleReleaseSnapshot> loadFull() {
        long cursor = dataService.maxReleaseCursor(tenantId, scene);
        Map<String, SpelRuleReleaseSnapshot> snapshots = new LinkedHashMap<>();
        for (SpelRuleReleaseData release : dataService.listLatestReleases(tenantId, scene)) {
            if (!release.deleted()) {
                SpelRuleReleaseSnapshot snapshot = parseAndValidate(release);
                snapshots.put(snapshot.ruleSetCode(), snapshot);
            }
        }
        return new FullLoadResult<>(snapshots, cursor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncrementalChange<String, SpelRuleReleaseSnapshot>> loadChanges(long afterCursor, int limit) {
        List<IncrementalChange<String, SpelRuleReleaseSnapshot>> changes = new ArrayList<>();
        for (SpelRuleReleaseData release : dataService.listReleaseChanges(tenantId, scene, afterCursor, limit)) {
            if (release.deleted()) {
                changes.add(new IncrementalChange<>(release.releaseId(), ChangeOperation.DELETE,
                        release.ruleSetCode(), null));
            } else {
                changes.add(new IncrementalChange<>(release.releaseId(), ChangeOperation.UPSERT,
                        release.ruleSetCode(), parseAndValidate(release)));
            }
        }
        return List.copyOf(changes);
    }

    private SpelRuleReleaseSnapshot parseAndValidate(SpelRuleReleaseData release) {
        if (!sha256(release.snapshotJson()).equals(release.checksum())) {
            throw new IllegalArgumentException("SpEL rule release checksum does not match: " + release.releaseId());
        }
        try {
            SpelRuleReleaseSnapshot snapshot = objectMapper.readValue(
                    release.snapshotJson(), SpelRuleReleaseSnapshot.class);
            if (!release.ruleSetCode().equals(snapshot.ruleSetCode())
                    || !release.scene().equals(snapshot.scene())
                    || release.releaseVersion() != snapshot.releaseVersion()) {
                throw new IllegalArgumentException("SpEL rule release metadata does not match: "
                        + release.releaseId());
            }
            return snapshot;
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse SpEL rule release: " + release.releaseId(), exception);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}
