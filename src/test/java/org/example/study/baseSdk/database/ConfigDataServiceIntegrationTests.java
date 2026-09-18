package org.example.study.baseSdk.database;

import org.example.study.baseSdk.database.api.model.SaveConfigItemCommand;
import org.example.study.baseSdk.database.api.service.ConfigDataService;
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

    @Test
    void savesQueriesAndDisablesConfigItem() {
        var saved = configDataService.save(new SaveConfigItemCommand(
                NAMESPACE, CONFIG_KEY, "{\"enabled\":true}", true, "Database SDK integration test"
        ));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.version()).isPositive();
        assertThat(configDataService.findEnabled(NAMESPACE, CONFIG_KEY)).isPresent();
        assertThat(configDataService.listEnabled(NAMESPACE)).extracting("configKey").contains(CONFIG_KEY);
        assertThat(configDataService.disable(NAMESPACE, CONFIG_KEY)).isTrue();
        assertThat(configDataService.findEnabled(NAMESPACE, CONFIG_KEY)).isEmpty();
    }
}
