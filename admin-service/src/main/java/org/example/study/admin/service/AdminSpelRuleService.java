package org.example.study.admin.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.example.study.admin.api.AdminApiException;
import org.example.study.admin.api.PublishRuleSetRequest;
import org.example.study.admin.api.RuleValidationResult;
import org.example.study.admin.api.SaveRuleSetRequest;
import org.example.study.admin.api.SaveSpelRuleRequest;
import org.example.study.admin.security.AdminSecurityContext;
import org.example.study.baseSdk.database.api.exception.ConfigConflictException;
import org.example.study.baseSdk.database.api.model.SaveSpelRuleDraftCommand;
import org.example.study.baseSdk.database.api.model.SaveSpelRuleSetCommand;
import org.example.study.baseSdk.database.api.model.SpelRuleDraftData;
import org.example.study.baseSdk.database.api.model.SpelRuleReleaseData;
import org.example.study.baseSdk.database.api.model.SpelRuleSetData;
import org.example.study.baseSdk.database.api.service.SpelRuleDataService;
import org.example.study.baseSdk.rule.spel.SpelRuleDefinition;
import org.example.study.baseSdk.rule.spel.SpelRuleEngine;
import org.example.study.baseSdk.rule.spel.SpelRuleErrorPolicy;
import org.example.study.baseSdk.rule.spel.SpelRuleEvaluationMode;
import org.example.study.baseSdk.rule.spel.SpelRuleReleaseSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * 管理台 SpEL 规则编排边界，负责校验、发布和回滚完整规则快照。
 */
@Service
public class AdminSpelRuleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminSpelRuleService.class);
    private static final Set<String> SCENES = Set.of("TEXT", "IMAGE", "AUDIO", "VIDEO");

    private final SpelRuleDataService dataService;
    private final SpelRuleEngine ruleEngine;
    private final ObjectMapper objectMapper;

    public AdminSpelRuleService(SpelRuleDataService dataService, SpelRuleEngine ruleEngine,
                                ObjectMapper objectMapper) {
        this.dataService = dataService;
        this.ruleEngine = ruleEngine;
        this.objectMapper = objectMapper;
    }

    public List<SpelRuleSetData> listRuleSets() {
        return dataService.listRuleSets(tenantId());
    }

    public List<SpelRuleDraftData> listRules(String ruleSetCode) {
        requireRuleSet(ruleSetCode);
        return dataService.listDrafts(tenantId(), ruleSetCode);
    }

    public List<SpelRuleReleaseData> listReleases(String ruleSetCode) {
        requireRuleSet(ruleSetCode);
        return dataService.listReleases(tenantId(), ruleSetCode);
    }

    public SpelRuleSetData saveRuleSet(String ruleSetCode, SaveRuleSetRequest request) {
        validateIdentifier(ruleSetCode);
        if (request == null || request.name() == null || request.name().isBlank() || request.name().length() > 128
                || request.scene() == null || !SCENES.contains(request.scene())
                || request.evaluationMode() == null || request.errorPolicy() == null
                || request.enabled() == null || request.version() == null || request.version() < 0) {
            throw invalidRequest();
        }
        parseMode(request.evaluationMode());
        parsePolicy(request.errorPolicy());
        try {
            return dataService.saveRuleSet(new SaveSpelRuleSetCommand(tenantId(), ruleSetCode,
                    request.name(), request.scene(),
                    request.evaluationMode(), request.errorPolicy(), request.enabled()), request.version());
        } catch (ConfigConflictException exception) {
            throw conflict();
        }
    }

    @Transactional
    public SpelRuleDraftData saveRule(String ruleSetCode, String ruleId, SaveSpelRuleRequest request) {
        validateIdentifier(ruleSetCode);
        validateIdentifier(ruleId);
        if (request == null || request.name() == null || request.name().isBlank() || request.name().length() > 128
                || request.expression() == null || request.expression().isBlank()
                || request.expression().length() > SpelRuleEngine.DEFAULT_MAX_EXPRESSION_LENGTH
                || request.priority() == null || request.enabled() == null
                || request.attributes() == null || request.version() == null || request.version() < 0) {
            throw invalidRequest();
        }
        try {
            dataService.lockRuleSet(tenantId(), ruleSetCode);
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
        try {
            SpelRuleDefinition definition = new SpelRuleDefinition(ruleId, request.expression(), request.priority(),
                    true, request.attributes());
            ruleEngine.compile(List.of(definition));
            return dataService.saveDraft(tenantId(), ruleSetCode, new SaveSpelRuleDraftCommand(ruleId, request.name(),
                    request.expression(), request.priority(), writeJson(request.attributes()), request.enabled()),
                    request.version());
        } catch (ConfigConflictException exception) {
            throw conflict();
        } catch (IllegalArgumentException exception) {
            throw ruleInvalid(exception);
        }
    }

    @Transactional(readOnly = true)
    public RuleValidationResult validate(String ruleSetCode) {
        SpelRuleSetData ruleSet = requireRuleSet(ruleSetCode);
        try {
            List<SpelRuleDefinition> definitions = definitions(dataService.listDrafts(tenantId(), ruleSetCode));
            validateAllExpressions(definitions);
            int count = ruleEngine.compile(definitions).size();
            return new RuleValidationResult(ruleSet.ruleSetCode(), count, true);
        } catch (IllegalArgumentException exception) {
            throw ruleInvalid(exception);
        }
    }

    @Transactional
    public SpelRuleReleaseData publish(String ruleSetCode, PublishRuleSetRequest request) {
        validatePublishRequest(request);
        SpelRuleSetData ruleSet = lockAndCheckVersion(ruleSetCode, request.version());
        SpelRuleReleaseSnapshot snapshot = buildSnapshot(ruleSet, ruleSet.publishedVersion() + 1,
                definitions(dataService.listDrafts(tenantId(), ruleSetCode)));
        return persistSnapshot(ruleSet, snapshot, AdminSecurityContext.requirePrincipal().username());
    }

    @Transactional
    public SpelRuleReleaseData rollback(String ruleSetCode, long sourceVersion, PublishRuleSetRequest request) {
        validatePublishRequest(request);
        if (sourceVersion <= 0) {
            throw invalidRequest();
        }
        SpelRuleSetData ruleSet = lockAndCheckVersion(ruleSetCode, request.version());
        SpelRuleReleaseData source = dataService.findRelease(tenantId(), ruleSetCode, sourceVersion)
                .orElseThrow(() -> new AdminApiException("RULE_RELEASE_NOT_FOUND",
                        "SpEL rule release not found", HttpStatus.NOT_FOUND));
        SpelRuleReleaseSnapshot oldSnapshot = readSnapshot(source.snapshotJson());
        SpelRuleReleaseSnapshot nextSnapshot = new SpelRuleReleaseSnapshot(ruleSet.ruleSetCode(), ruleSet.scene(),
                ruleSet.publishedVersion() + 1, oldSnapshot.evaluationMode(), oldSnapshot.errorPolicy(),
                oldSnapshot.enabled(), oldSnapshot.rules());
        try {
            validateAllExpressions(nextSnapshot.rules());
        } catch (IllegalArgumentException exception) {
            throw ruleInvalid(exception);
        }
        return persistSnapshot(ruleSet, nextSnapshot, AdminSecurityContext.requirePrincipal().username());
    }

    private SpelRuleReleaseData persistSnapshot(SpelRuleSetData ruleSet, SpelRuleReleaseSnapshot snapshot,
                                                String publishedBy) {
        String json = writeJson(snapshot);
        try {
            SpelRuleReleaseData release = dataService.publish(tenantId(), ruleSet.ruleSetCode(), ruleSet.version(),
                    snapshot.releaseVersion(), json, sha256(json), !snapshot.enabled(), publishedBy);
            LOGGER.info("SpEL rule set published, ruleSetCode={}, releaseVersion={}, releaseId={}",
                    release.ruleSetCode(), release.releaseVersion(), release.releaseId());
            return release;
        } catch (ConfigConflictException exception) {
            throw conflict();
        }
    }

    private SpelRuleReleaseSnapshot buildSnapshot(SpelRuleSetData ruleSet, long releaseVersion,
                                                  List<SpelRuleDefinition> definitions) {
        try {
            validateAllExpressions(definitions);
            ruleEngine.compile(definitions);
        } catch (IllegalArgumentException exception) {
            throw ruleInvalid(exception);
        }
        return new SpelRuleReleaseSnapshot(ruleSet.ruleSetCode(), ruleSet.scene(), releaseVersion,
                parseMode(ruleSet.evaluationMode()), parsePolicy(ruleSet.errorPolicy()), ruleSet.enabled(), definitions);
    }

    private List<SpelRuleDefinition> definitions(List<SpelRuleDraftData> drafts) {
        List<SpelRuleDefinition> definitions = new ArrayList<>();
        for (SpelRuleDraftData draft : drafts) {
            definitions.add(new SpelRuleDefinition(draft.ruleId(), draft.expression(), draft.priority(),
                    draft.enabled(), readAttributes(draft.attributesJson())));
        }
        return List.copyOf(definitions);
    }

    private void validateAllExpressions(List<SpelRuleDefinition> definitions) {
        List<SpelRuleDefinition> enabledDefinitions = definitions.stream()
                .map(rule -> new SpelRuleDefinition(rule.ruleId(), rule.expression(), rule.priority(),
                        true, rule.attributes()))
                .toList();
        ruleEngine.compile(enabledDefinitions);
    }

    private Map<String, String> readAttributes(String json) {
        try {
            Map<?, ?> raw = objectMapper.readValue(json, Map.class);
            Map<String, String> attributes = new LinkedHashMap<>();
            raw.forEach((key, value) -> {
                if (!(key instanceof String) || !(value instanceof String)) {
                    throw new IllegalArgumentException("SpEL rule attributes must contain string values");
                }
                attributes.put((String) key, (String) value);
            });
            return Map.copyOf(attributes);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse SpEL rule attributes", exception);
        }
    }

    private SpelRuleReleaseSnapshot readSnapshot(String json) {
        try {
            return objectMapper.readValue(json, SpelRuleReleaseSnapshot.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse SpEL rule release snapshot", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to serialize SpEL rule data", exception);
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

    private SpelRuleSetData lockAndCheckVersion(String ruleSetCode, long expectedVersion) {
        validateIdentifier(ruleSetCode);
        SpelRuleSetData ruleSet;
        try {
            ruleSet = dataService.lockRuleSet(tenantId(), ruleSetCode);
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
        if (!ruleSet.version().equals(expectedVersion)) {
            throw conflict();
        }
        return ruleSet;
    }

    private SpelRuleSetData requireRuleSet(String ruleSetCode) {
        validateIdentifier(ruleSetCode);
        return dataService.findRuleSet(tenantId(), ruleSetCode).orElseThrow(this::notFound);
    }

    private void validatePublishRequest(PublishRuleSetRequest request) {
        if (request == null || request.version() == null || request.version() < 1) {
            throw invalidRequest();
        }
    }

    private void validateIdentifier(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")) {
            throw invalidRequest();
        }
    }

    private SpelRuleEvaluationMode parseMode(String value) {
        try {
            return SpelRuleEvaluationMode.valueOf(value);
        } catch (RuntimeException exception) {
            throw invalidRequest();
        }
    }

    private SpelRuleErrorPolicy parsePolicy(String value) {
        try {
            return SpelRuleErrorPolicy.valueOf(value);
        } catch (RuntimeException exception) {
            throw invalidRequest();
        }
    }

    private AdminApiException invalidRequest() {
        return new AdminApiException("INVALID_REQUEST", "Invalid SpEL rule request", HttpStatus.BAD_REQUEST);
    }

    private AdminApiException conflict() {
        return new AdminApiException("CONFIG_CONFLICT", "SpEL rule data was modified concurrently", HttpStatus.CONFLICT);
    }

    private AdminApiException notFound() {
        return new AdminApiException("RULE_SET_NOT_FOUND", "SpEL rule set not found", HttpStatus.NOT_FOUND);
    }

    private AdminApiException ruleInvalid(Exception cause) {
        return new AdminApiException("RULE_INVALID", "SpEL rule validation failed", HttpStatus.BAD_REQUEST, cause);
    }

    private long tenantId() {
        return AdminSecurityContext.requirePrincipal().tenantId();
    }
}
