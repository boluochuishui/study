package org.example.study.text.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 注册可被真实供应商客户端替换的默认大模型实现。 */
@Configuration(proxyBeanMethods = false)
public class TextLargeModelConfiguration {

    @Bean
    @ConditionalOnMissingBean(TextLargeModelClient.class)
    TextLargeModelClient noOpTextLargeModelClient() {
        return (taskId, normalizedText) -> TextLargeModelAssessment.pass();
    }
}
