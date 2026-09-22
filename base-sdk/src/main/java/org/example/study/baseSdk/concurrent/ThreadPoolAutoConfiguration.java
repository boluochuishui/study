package org.example.study.baseSdk.concurrent;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 baseSdk 线程池工厂。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolAutoConfiguration {

    @Bean
    public ManagedThreadPoolFactory managedThreadPoolFactory(ThreadPoolProperties properties) {
        return new ManagedThreadPoolFactory(properties);
    }
}
