package org.example.study.baseSdk.rule.spel;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 提供可由业务服务覆盖的默认规则引擎。
 */
@AutoConfiguration
public class SpelRuleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpelRuleEngine spelRuleEngine() {
        return new SpelRuleEngine();
    }
}
