package org.example.study.baseSdk.database.api.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 管理台可维护的业务表白名单；SQL 标识符只能来自此枚举。 */
public enum ManagedResource {
    APPS("apps", "detect_app", "app_id", "app_id", "app_name", "credential_ref", "description", "enabled"),
    SCENES("scenes", "detect_scene", "scene_code", "app_record_id", "scene_code", "scene_name",
            "default_language", "default_region", "description", "enabled"),
    RULES("rules", "detect_rule", "rule_code", "scene_id", "rule_code", "rule_name", "content_type",
            "rule_type", "execution_mode", "priority", "on_hit_action", "risk_labels_json", "parameters_json",
            "media_library_id", "spel_release_id", "spel_rule_id", "description", "enabled"),
    LIBRARIES("libraries", "media_library", "library_code", "library_code", "library_name", "content_type",
            "library_type", "description", "enabled"),
    ASSETS("assets", "media_asset", "content_sha256", "asset_name", "content_type", "content_sha256",
            "hash_version", "storage_status", "description", "enabled"),
    MEMBERSHIPS("memberships", "media_asset_library", "id", "library_id", "asset_id", "labels_json",
            "description", "enabled");

    private final String path;
    private final String table;
    private final String immutableColumn;
    private final List<String> columns;

    ManagedResource(String path, String table, String immutableColumn, String... columns) {
        this.path = path;
        this.table = table;
        this.immutableColumn = immutableColumn;
        this.columns = List.of(columns);
    }

    public String table() {
        return table;
    }

    public String immutableColumn() {
        return immutableColumn;
    }

    public List<String> columns() {
        return columns;
    }

    public static ManagedResource fromPath(String path) {
        return Arrays.stream(values()).filter(value -> value.path.equals(path)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown managed resource"));
    }

    public static Map<String, String> paths() {
        return Arrays.stream(values()).collect(Collectors.toMap(value -> value.path, value -> value.table));
    }
}
