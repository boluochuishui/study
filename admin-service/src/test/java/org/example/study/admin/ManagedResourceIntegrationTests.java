package org.example.study.admin;

import java.util.Map;
import java.util.UUID;
import org.example.study.baseSdk.database.api.audit.DatabaseAuditContext;
import org.example.study.baseSdk.database.api.exception.ConfigConflictException;
import org.example.study.baseSdk.database.api.model.ManagedResource;
import org.example.study.baseSdk.database.api.service.ManagedResourceDataService;
import org.example.study.baseSdk.database.mybatis.config.DatabaseMybatisConfiguration;
import org.example.study.baseSdk.database.mybatis.service.ManagedResourceDataServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 使用真实 MySQL 验证六表写入；测试事务统一回滚。 */
@SpringBootTest(classes = ManagedResourceIntegrationTests.DatabaseConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.config.import=optional:file:../application-local.yaml")
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "managed.integration.enabled", matches = "true")
@Transactional
class ManagedResourceIntegrationTests {
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({DatabaseMybatisConfiguration.class, ManagedResourceDataServiceImpl.class})
    static class DatabaseConfiguration { }

    @Autowired
    private ManagedResourceDataService data;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsAndUpdatesSixResourcesWithAuditAndTenantIsolation() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long tenantId = 1;
        long otherTenant = createTenant(suffix);
        Map<String, Object> app;
        Map<String, Object> scene;
        Map<String, Object> library;
        Map<String, Object> asset;
        Map<String, Object> membership;
        Map<String, Object> rule;
        try (var scope = DatabaseAuditContext.open(101)) {
            app = data.create(tenantId, ManagedResource.APPS, Map.of(
                    "app_id", "managed-" + suffix, "app_name", "Managed app", "credential_ref", "env:TEST_APP"));
            scene = data.create(tenantId, ManagedResource.SCENES, Map.of(
                    "app_record_id", id(app), "scene_code", "text-" + suffix, "scene_name", "Text"));
            library = data.create(tenantId, ManagedResource.LIBRARIES, Map.of(
                    "library_code", "library-" + suffix, "library_name", "Bad text",
                    "content_type", "TEXT", "library_type", "BLOCKLIST"));
            asset = data.create(tenantId, ManagedResource.ASSETS, Map.of(
                    "asset_name", "Sample", "content_type", "TEXT", "content_sha256", "a".repeat(64),
                    "hash_version", "text-normalize-v1", "storage_status", "READY"));
            membership = data.create(tenantId, ManagedResource.MEMBERSHIPS, Map.of(
                    "library_id", id(library), "asset_id", id(asset), "labels_json", "[\"risk\"]"));
            rule = data.create(tenantId, ManagedResource.RULES, Map.of(
                    "scene_id", id(scene), "rule_code", "hash-" + suffix, "rule_name", "Hash match",
                    "content_type", "TEXT", "rule_type", "MEDIA_HASH", "execution_mode", "BOTH",
                    "on_hit_action", "REJECT", "media_library_id", id(library)));
        }
        assertThat(data.list(tenantId, ManagedResource.APPS)).extracting(row -> ((Number) row.get("id")).longValue())
                .contains(id(app));
        assertThat(data.list(otherTenant, ManagedResource.APPS)).extracting(row -> ((Number) row.get("id")).longValue())
                .doesNotContain(id(app));
        assertThat(data.find(otherTenant, ManagedResource.APPS, id(app))).isNull();
        assertThat(data.hasEnabledReference(tenantId, ManagedResource.SCENES, "app_record_id", id(app)))
                .isTrue();
        assertThat(data.hasEnabledReference(otherTenant, ManagedResource.SCENES, "app_record_id", id(app)))
                .isFalse();
        assertThat(jdbc.queryForObject("SELECT created_by FROM detect_rule WHERE id = ?", Long.class, id(rule)))
                .isEqualTo(101);
        assertThat(jdbc.queryForObject("SELECT created_by FROM media_asset_library WHERE id = ?", Long.class,
                id(membership))).isEqualTo(101);
        try (var scope = DatabaseAuditContext.open(102)) {
            Map<String, Object> updated = data.update(tenantId, ManagedResource.APPS, id(app), 1,
                    Map.of("app_name", "Changed app", "enabled", 0));
            assertThat(updated.get("version")).isEqualTo(2L);
        }
        assertThat(jdbc.queryForObject("SELECT updated_by FROM detect_app WHERE id = ?", Long.class, id(app)))
                .isEqualTo(102);
        assertThatThrownBy(() -> data.update(tenantId, ManagedResource.APPS, id(app), 1,
                Map.of("app_name", "Stale"))).isInstanceOf(ConfigConflictException.class);
        assertThatThrownBy(() -> data.create(otherTenant, ManagedResource.SCENES, Map.of(
                "app_record_id", id(app), "scene_code", "invalid-" + suffix, "scene_name", "Invalid")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private long createTenant(String suffix) {
        jdbc.update("INSERT INTO admin_tenant (tenant_code, tenant_name) VALUES (?, ?)",
                "managed-" + suffix, "Managed test tenant");
        return jdbc.queryForObject("SELECT id FROM admin_tenant WHERE tenant_code = ?", Long.class,
                "managed-" + suffix);
    }

    private long id(Map<String, Object> row) {
        return ((Number) row.get("id")).longValue();
    }
}
