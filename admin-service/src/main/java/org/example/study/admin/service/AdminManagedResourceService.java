package org.example.study.admin.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.example.study.admin.api.AdminApiException;
import org.example.study.admin.api.SaveManagedResourceRequest;
import org.example.study.admin.security.AdminSecurityContext;
import org.example.study.baseSdk.database.api.exception.ConfigConflictException;
import org.example.study.baseSdk.database.api.model.ManagedResource;
import org.example.study.baseSdk.database.api.service.ManagedResourceDataService;
import org.example.study.baseSdk.database.api.service.SpelRuleDataService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 管理台业务配置校验；租户身份由登录上下文决定。 */
@Service
public class AdminManagedResourceService {
    private static final Set<String> CONTENT_TYPES = Set.of("TEXT", "IMAGE", "AUDIO", "VIDEO");
    private static final Set<String> LIBRARY_TYPES = Set.of("BLOCKLIST", "ALLOWLIST", "TESTSET");
    private static final Set<String> RULE_TYPES = Set.of("KEYWORD", "REGEX", "MEDIA_HASH", "SPEL", "LLM");
    private static final Set<String> ACTIONS = Set.of("PASS", "REVIEW", "REJECT");
    private static final Set<String> MODES = Set.of("BOTH", "SYNC", "ASYNC");
    private static final Set<String> STATUSES = Set.of("PENDING", "READY", "FAILED");
    private final ManagedResourceDataService data;
    private final SpelRuleDataService spelData;

    public AdminManagedResourceService(ManagedResourceDataService data, SpelRuleDataService spelData) {
        this.data = data;
        this.spelData = spelData;
    }

    public List<Map<String, Object>> list(String path) {
        return data.list(tenantId(), ManagedResource.fromPath(path));
    }

    public Map<String, Object> create(String path, SaveManagedResourceRequest body) {
        ManagedResource resource = ManagedResource.fromPath(path);
        if (body == null || body.getVersion() == null || body.getVersion() != 0) {
            throw invalid();
        }
        Map<String, Object> values = checkedValues(resource, body.getValues());
        validate(resource, values);
        return data.create(tenantId(), resource, values);
    }

    public Map<String, Object> update(String path, long id, SaveManagedResourceRequest body) {
        ManagedResource resource = ManagedResource.fromPath(path);
        if (body == null || body.getVersion() == null || body.getVersion() < 1) {
            throw invalid();
        }
        Map<String, Object> current = data.find(tenantId(), resource, id);
        if (current == null) {
            throw new AdminApiException("MANAGED_NOT_FOUND", "Managed resource not found", HttpStatus.NOT_FOUND);
        }
        Map<String, Object> values = checkedValues(resource, body.getValues());
        for (String key : List.of(resource.immutableColumn(), "app_record_id", "scene_id", "library_id", "asset_id")) {
            if (values.containsKey(key) && !String.valueOf(values.get(key)).equals(String.valueOf(current.get(key)))) {
                throw invalid();
            }
        }
        if (Set.of(ManagedResource.LIBRARIES, ManagedResource.ASSETS).contains(resource)) {
            for (String key : resource == ManagedResource.ASSETS
                    ? List.of("content_type", "hash_version") : List.of("content_type")) {
                if (values.containsKey(key) && !String.valueOf(values.get(key)).equals(String.valueOf(current.get(key)))) {
                    throw invalid();
                }
            }
        }
        Map<String, Object> merged = new LinkedHashMap<>(current);
        merged.putAll(values);
        validate(resource, merged);
        if (values.containsKey("enabled") && !isEnabled(merged)) {
            validateDeactivate(resource, id);
        }
        try {
            return data.update(tenantId(), resource, id, body.getVersion(), values);
        } catch (ConfigConflictException exception) {
            throw new AdminApiException("CONFIG_CONFLICT", "Managed resource was modified concurrently",
                    HttpStatus.CONFLICT, exception);
        }
    }

    private Map<String, Object> checkedValues(ManagedResource resource, Map<String, Object> input) {
        if (input == null || input.isEmpty() || !resource.columns().containsAll(input.keySet())) {
            throw invalid();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (String column : resource.columns()) {
            if (!input.containsKey(column)) {
                continue;
            }
            Object value = input.get(column);
            if (column.equals("enabled")) {
                if (!(value instanceof Boolean)) {
                    throw invalid();
                }
                value = (Boolean) value ? 1 : 0;
            } else if (column.equals("priority") || Set.of("app_record_id", "scene_id", "library_id",
                    "asset_id", "media_library_id", "spel_release_id").contains(column)) {
                if (value != null) {
                    if (!(value instanceof Number number)) {
                        throw invalid();
                    }
                    if (!Double.isFinite(number.doubleValue()) || number.doubleValue() != number.longValue()) {
                        throw invalid();
                    }
                    if (column.equals("priority")) {
                        if (number.longValue() < 0 || number.longValue() > Integer.MAX_VALUE) {
                            throw invalid();
                        }
                    } else if (number.longValue() < 1) {
                        throw invalid();
                    }
                }
            } else if (column.endsWith("_json")) {
                value = checkedJson(column, value);
            } else if (value != null) {
                if (!(value instanceof String) || ((String) value).length() > 512) {
                    throw invalid();
                }
                value = ((String) value).trim();
                if (((String) value).isEmpty()) {
                    value = null;
                }
            }
            values.put(column, value);
        }
        return values;
    }

    private String checkedJson(String column, Object value) {
        if (value == null || value instanceof String text && text.isBlank()) {
            return null;
        }
        try {
            Object parsed = value instanceof String text ? JSON.parse(text) : value;
            if (column.equals("parameters_json") ? !(parsed instanceof JSONObject || parsed instanceof Map)
                    : !(parsed instanceof JSONArray || parsed instanceof List)) {
                throw invalid();
            }
            String json = JSON.toJSONString(parsed);
            if (json.length() > 8192) {
                throw invalid();
            }
            return json;
        } catch (RuntimeException exception) {
            throw invalid();
        }
    }

    private void validate(ManagedResource resource, Map<String, Object> values) {
        switch (resource) {
            case APPS -> {
                requireCode(values, "app_id");
                requireText(values, "app_name");
                String ref = text(values, "credential_ref");
                if (ref == null || !(ref.startsWith("env:") || ref.startsWith("secret:"))) {
                    throw invalid();
                }
            }
            case SCENES -> {
                requireReference(ManagedResource.APPS, values.get("app_record_id"));
                requireCode(values, "scene_code");
                requireText(values, "scene_name");
            }
            case LIBRARIES -> {
                requireCode(values, "library_code");
                requireText(values, "library_name");
                requireOption(values, "content_type", CONTENT_TYPES);
                requireOption(values, "library_type", LIBRARY_TYPES);
            }
            case ASSETS -> {
                requireText(values, "asset_name");
                requireOption(values, "content_type", CONTENT_TYPES);
                String hash = text(values, "content_sha256");
                if (hash == null || !hash.matches("[0-9a-f]{64}")) {
                    throw invalid();
                }
                requireText(values, "hash_version");
                requireOption(values, "storage_status", STATUSES);
            }
            case MEMBERSHIPS -> {
                Map<String, Object> library = requireReference(ManagedResource.LIBRARIES, values.get("library_id"));
                Map<String, Object> asset = requireReference(ManagedResource.ASSETS, values.get("asset_id"));
                if (!library.get("content_type").equals(asset.get("content_type"))
                        || !"READY".equals(asset.get("storage_status"))) {
                    throw invalid();
                }
            }
            case RULES -> validateRule(values);
            default -> throw invalid();
        }
    }

    private void validateRule(Map<String, Object> values) {
        requireReference(ManagedResource.SCENES, values.get("scene_id"));
        requireCode(values, "rule_code");
        requireText(values, "rule_name");
        requireOption(values, "content_type", CONTENT_TYPES);
        requireOption(values, "rule_type", RULE_TYPES);
        requireOption(values, "execution_mode", MODES);
        String type = text(values, "rule_type");
        if ("LLM".equals(type) && !"ASYNC".equals(values.get("execution_mode"))) {
            throw invalid();
        }
        if ("MEDIA_HASH".equals(type)) {
            Map<String, Object> library = requireReference(ManagedResource.LIBRARIES, values.get("media_library_id"));
            if (!library.get("content_type").equals(values.get("content_type"))
                    || "TESTSET".equals(library.get("library_type"))) {
                throw invalid();
            }
            String action = text(values, "on_hit_action");
            if ("ALLOWLIST".equals(library.get("library_type")) ? !"PASS".equals(action)
                    : !Set.of("REJECT", "REVIEW").contains(action)) {
                throw invalid();
            }
        } else if (values.get("media_library_id") != null) {
            throw invalid();
        }
        if ("SPEL".equals(type)) {
            if (values.get("spel_release_id") == null || text(values, "spel_rule_id") == null
                    || values.get("on_hit_action") != null || values.get("risk_labels_json") != null
                    || values.get("parameters_json") != null) {
                throw invalid();
            }
            long releaseId = ((Number) values.get("spel_release_id")).longValue();
            var release = spelData.findReleaseById(tenantId(), releaseId).orElseThrow(this::invalid);
            JSONObject snapshot = JSON.parseObject(release.snapshotJson());
            JSONArray rules = snapshot.getJSONArray("rules");
            if (!values.get("content_type").equals(snapshot.getString("scene"))
                    || !Boolean.TRUE.equals(snapshot.getBoolean("enabled")) || rules == null
                    || rules.stream().noneMatch(rule -> rule instanceof JSONObject item
                    && values.get("spel_rule_id").equals(item.getString("ruleId")))) {
                throw invalid();
            }
        } else {
            if (values.get("spel_release_id") != null || values.get("spel_rule_id") != null) {
                throw invalid();
            }
            requireOption(values, "on_hit_action", ACTIONS);
            if (Set.of("KEYWORD", "REGEX", "LLM").contains(type) && values.get("parameters_json") == null) {
                throw invalid();
            }
        }
    }

    private void validateDeactivate(ManagedResource resource, long id) {
        ManagedResource child = switch (resource) {
            case APPS -> ManagedResource.SCENES;
            case SCENES -> ManagedResource.RULES;
            case LIBRARIES -> ManagedResource.MEMBERSHIPS;
            case ASSETS -> ManagedResource.MEMBERSHIPS;
            default -> null;
        };
        if (child == null) {
            return;
        }
        String reference = switch (resource) {
            case APPS -> "app_record_id";
            case SCENES -> "scene_id";
            case LIBRARIES -> "library_id";
            case ASSETS -> "asset_id";
            default -> throw invalid();
        };
        if (data.hasEnabledReference(tenantId(), child, reference, id)) {
            throw new AdminApiException("CONFIG_CONFLICT", "Disable dependent records first", HttpStatus.CONFLICT);
        }
        if (resource == ManagedResource.LIBRARIES && data.hasEnabledReference(tenantId(),
                ManagedResource.RULES, "media_library_id", id)) {
            throw new AdminApiException("CONFIG_CONFLICT", "Disable dependent rules first", HttpStatus.CONFLICT);
        }
    }

    private Map<String, Object> requireReference(ManagedResource resource, Object id) {
        if (!(id instanceof Number number) || number.longValue() < 1) {
            throw invalid();
        }
        Map<String, Object> row = data.find(tenantId(), resource, number.longValue());
        if (row == null || !isEnabled(row)) {
            throw invalid();
        }
        return row;
    }

    private boolean isEnabled(Map<String, Object> row) {
        Object enabled = row.get("enabled");
        return enabled instanceof Boolean flag ? flag : enabled instanceof Number number && number.intValue() == 1;
    }

    private void requireOption(Map<String, Object> values, String key, Set<String> options) {
        String value = text(values, key);
        if (value == null || !options.contains(value)) {
            throw invalid();
        }
    }

    private void requireCode(Map<String, Object> values, String key) {
        String value = text(values, key);
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")) {
            throw invalid();
        }
    }

    private void requireText(Map<String, Object> values, String key) {
        if (text(values, key) == null) {
            throw invalid();
        }
    }

    private String text(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private long tenantId() {
        return AdminSecurityContext.requirePrincipal().tenantId();
    }

    private AdminApiException invalid() {
        return new AdminApiException("INVALID_REQUEST", "Invalid managed resource", HttpStatus.BAD_REQUEST);
    }
}
