package org.example.study.rule.spel;

import org.example.study.baseSdk.database.api.service.SpelRuleDataService;
import org.example.study.baseSdk.rule.spel.SpelRuleEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/** 按配置启用当前模态的数据库 SpEL 规则热加载。 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "detect.rules.spel.enabled", havingValue = "true")
public class SpelRuleHotLoadConfiguration {

    @Bean
    public SpelRuleReleaseSource spelRuleReleaseSource(SpelRuleDataService dataService, ObjectMapper objectMapper,
                                                       @Value("${detect.rules.spel.tenant-id:1}") long tenantId,
                                                       @Value("${detect.rules.spel.scene}") String scene) {
        return new SpelRuleReleaseSource(dataService, objectMapper, tenantId, scene);
    }

    @Bean
    public SpelRuleHotLoadModule spelRuleHotLoadModule(SpelRuleReleaseSource source, SpelRuleEngine ruleEngine) {
        return new SpelRuleHotLoadModule(source, ruleEngine);
    }
}
