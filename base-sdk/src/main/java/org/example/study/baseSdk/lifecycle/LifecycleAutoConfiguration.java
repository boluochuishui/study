package org.example.study.baseSdk.lifecycle;

import java.util.List;

import org.example.study.baseSdk.concurrent.ManagedThreadPoolFactory;
import org.example.study.baseSdk.hotload.HotLoadManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/** 所有服务共享生命周期协调能力，不依赖 RPC 或 MQ 框架。 */
@AutoConfiguration
@EnableConfigurationProperties(LifecycleProperties.class)
public class LifecycleAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ServiceLifecycleManager serviceLifecycleManager(List<LifecycleParticipant> participants, LifecycleProperties properties) {
        return new ServiceLifecycleManager(participants, properties);
    }

    @Bean
    @Order(100)
    public LifecycleParticipant infrastructureLifecycleParticipant(ObjectProvider<HotLoadManager> hotLoad,
                                                                  ObjectProvider<ManagedThreadPoolFactory> pools) {
        return new LifecycleParticipant() {
            @Override
            public void prepare() {
                HotLoadManager manager = hotLoad.getIfAvailable();
                if (manager != null && !manager.initialized()) {
                    throw new IllegalStateException("Required hot load snapshots are not initialized");
                }
            }

            @Override
            public void afterDrain(java.time.Duration remaining) {
                ManagedThreadPoolFactory factory = pools.getIfAvailable();
                if (factory != null) {
                    factory.shutdown(remaining);
                }
            }
        };
    }
}
