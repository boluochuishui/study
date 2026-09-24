package org.example.study.baseSdk.database;

import org.example.study.baseSdk.database.api.model.SaveConfigItemCommand;
import org.example.study.baseSdk.database.api.service.ConfigDataService;
import org.example.study.baseSdk.auth.AuthMode;
import org.example.study.baseSdk.auth.DatabaseAuthConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用本地 MySQL 验证数据库 SDK 的真实读写，仅在显式启用时执行。
 */
@SpringBootTest
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "database.integration.enabled", matches = "true")
@Sql(scripts = "/sql/cleanup_config_data_service_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ConfigDataServiceIntegrationTests {

    private static final String NAMESPACE = "integration-test";
    private static final String CONFIG_KEY = "database-sdk";

    @Autowired
    private ConfigDataService configDataService;

    @Autowired
    private DatabaseAuthConfigSource authConfigSource;

    @Test
    void savesQueriesAndDisablesConfigItem() {
        var saved = configDataService.save(new SaveConfigItemCommand(
                1, NAMESPACE, CONFIG_KEY, "{\"enabled\":true}", true, "Database SDK integration test"
        ));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.version()).isPositive();
        assertThat(configDataService.findEnabled(1, NAMESPACE, CONFIG_KEY)).isPresent();
        assertThat(configDataService.listEnabled(1, NAMESPACE)).extracting("configKey").contains(CONFIG_KEY);
        assertThat(configDataService.disable(1, NAMESPACE, CONFIG_KEY)).isTrue();
        assertThat(configDataService.findEnabled(1, NAMESPACE, CONFIG_KEY)).isEmpty();
    }

    @Test
    void readsSeededAuthenticationSnapshot() {
        var snapshot = authConfigSource.loadLatest();

        assertThat(snapshot).isPresent();
        assertThat(snapshot.orElseThrow().version()).isEqualTo("db-v1");
        assertThat(snapshot.orElseThrow().rules())
                .anySatisfy(rule -> {
                    assertThat(rule.ruleId()).isEqualTo("detect-sync");
                    assertThat(rule.authMode()).isEqualTo(AuthMode.SIGNATURE);
                });
    }
}
