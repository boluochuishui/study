package org.example.study.baseSdk.hotload;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 注册热加载配置并启用定时任务。
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(HotLoadProperties.class)
public class HotLoadConfiguration {
}
