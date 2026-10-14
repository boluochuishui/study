package org.example.study.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.example.study.baseSdk.database.api.audit.DatabaseAuditContext;
import org.example.study.baseSdk.database.api.model.OnlineTestRecordData;
import org.example.study.baseSdk.database.api.model.SaveConfigItemCommand;
import org.example.study.baseSdk.database.api.model.SaveSpelRuleDraftCommand;
import org.example.study.baseSdk.database.api.model.SaveSpelRuleSetCommand;
import org.example.study.baseSdk.database.api.service.AdminSecurityDataService;
import org.example.study.baseSdk.database.api.service.ConfigDataService;
import org.example.study.baseSdk.database.api.service.OnlineTestRecordDataService;
import org.example.study.baseSdk.database.api.service.SpelRuleDataService;
import org.example.study.baseSdk.database.mybatis.config.DatabaseMetaObjectHandler;
import org.example.study.baseSdk.database.mybatis.config.DatabaseMybatisConfiguration;
import org.example.study.baseSdk.database.mybatis.service.AdminSecurityDataServiceImpl;
import org.example.study.baseSdk.database.mybatis.service.ConfigDataServiceImpl;
import org.example.study.baseSdk.database.mybatis.service.OnlineTestRecordDataServiceImpl;
import org.example.study.baseSdk.database.mybatis.service.SpelRuleDataServiceImpl;
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

/** 使用本机 MySQL 验证实际 SQL 与自动填充，所有写入在测试事务结束时回滚。 */
@SpringBootTest(classes = DatabaseAuditIntegrationTests.DatabaseConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.config.import=optional:file:../application-local.yaml")
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "database.audit.integration.enabled", matches = "true")
@Transactional
class DatabaseAuditIntegrationTests {
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({DatabaseMybatisConfiguration.class, DatabaseMetaObjectHandler.class,
            ConfigDataServiceImpl.class, AdminSecurityDataServiceImpl.class,
            SpelRuleDataServiceImpl.class, OnlineTestRecordDataServiceImpl.class})
    static class DatabaseConfiguration {
    }

    @Autowired
    private ConfigDataService configs;
    @Autowired
    private AdminSecurityDataService security;
    @Autowired
    private SpelRuleDataService spel;
    @Autowired
    private OnlineTestRecordDataService records;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void auditsMybatisPlusAndXmlWritesWithoutChangingCreators() {
        long tenant = security.requireTenantId("default");
        String code = "audit." + UUID.randomUUID();
        long userId;
        long configId;
        long roleId;
        long setId;
        long releaseId;
        LocalDateTime createdAt;
        try (var scope = DatabaseAuditContext.open(101)) {
            userId = security.createUser(tenant, code, "Audit test", "test-only-hash").id();
            configs.saveIfVersion(new SaveConfigItemCommand(tenant, "audit-test", code, "v1", true, "test"), 0);
            var config = configs.find(tenant, "audit-test", code).orElseThrow();
            configId = config.id();
            createdAt = config.createdAt();
            roleId = security.saveRole(tenant, code, "Audit role", true, List.of("TEST_READ")).id();
            security.replaceUserRoles(tenant, userId, List.of(code));
            setId = spel.saveRuleSet(new SaveSpelRuleSetCommand(tenant, code, "Audit rules", "TEXT",
                    "FIRST_MATCH", "CONTINUE", true), 0).id();
            var draft = spel.saveDraft(tenant, code,
                    new SaveSpelRuleDraftCommand("rule1", "Audit rule", "true", 1, "{}", true), 0);
            assertAudit("sdk_spel_rule_draft", "id", draft.id(), 101, 101);
            releaseId = spel.publish(tenant, code, 1, 1, "{}", "0".repeat(64), false, "Audit test").releaseId();
        }
        assertAudit("admin_user", "id", userId, 101, 101);
        assertAudit("admin_role", "id", roleId, 101, 101);
        assertAudit("sdk_spel_rule_set", "id", setId, 101, 101);
        assertAudit("sdk_spel_rule_release", "release_id", releaseId, 101, 101);
        assertThat(jdbc.queryForObject("SELECT created_by FROM admin_user_role WHERE user_id = ?",
                Long.class, userId)).isEqualTo(101);
        assertThat(jdbc.queryForObject("SELECT created_by FROM admin_role_permission WHERE role_id = ?",
                Long.class, roleId)).isEqualTo(101);
        try (var scope = DatabaseAuditContext.open(102)) {
            configs.saveIfVersion(new SaveConfigItemCommand(tenant, "audit-test", code, "v2", true, "test"), 1);
            security.saveRole(tenant, code, "Updated audit role", true, List.of("TEST_READ"));
        }
        assertAudit("sdk_config_item", "id", configId, 101, 102);
        assertAudit("admin_role", "id", roleId, 101, 102);
        assertThat(configs.find(tenant, "audit-test", code).orElseThrow().createdAt()).isEqualTo(createdAt);

        // 登录和测试记录在没有请求线程身份时，仍使用已验证的用户编号。
        security.saveAccessToken(tenant, userId, code, LocalDateTime.now().plusHours(1));
        assertAudit("admin_access_token", "token_hash", code, userId, userId);
        try (var scope = DatabaseAuditContext.open(102)) {
            security.revokeAccessToken(code);
        }
        assertAudit("admin_access_token", "token_hash", code, userId, 102);
        records.save(new OnlineTestRecordData(null, tenant, userId, "TEXT", "SYNC", "test-app",
                "test-scene", 1, code, "COMPLETED", "{}", 1, LocalDateTime.now()));
        assertAudit("admin_online_test_record", "task_id", code, userId, userId);
        assertThat(records.list(tenant, 100)).anyMatch(record -> code.equals(record.taskId()));
        assertThat(DatabaseAuditContext.actorId()).isZero();
    }

    private void assertAudit(String table, String key, Object id, long creator, long updater) {
        var row = jdbc.queryForMap("SELECT created_by, created_at, updated_by, updated_at FROM "
                + table + " WHERE " + key + " = ?", id);
        assertThat(((Number) row.get("created_by")).longValue()).isEqualTo(creator);
        assertThat(((Number) row.get("updated_by")).longValue()).isEqualTo(updater);
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();
    }
}
