package org.example.study.baseSdk.concurrent;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * baseSdk 线程池配置，支持全局默认值和具名线程池覆盖值。
 */
@ConfigurationProperties(prefix = "base-sdk.thread-pool")
public class ThreadPoolProperties {

    private PoolConfig defaults = PoolConfig.sdkDefaults();
    private Map<String, PoolConfig> pools = new LinkedHashMap<>();

    public PoolConfig getDefaults() {
        return defaults;
    }

    public void setDefaults(PoolConfig defaults) {
        this.defaults = defaults;
    }

    public Map<String, PoolConfig> getPools() {
        return pools;
    }

    public void setPools(Map<String, PoolConfig> pools) {
        this.pools = pools;
    }

    /**
     * 可选配置项；空值表示继承全局默认配置。
     */
    public static class PoolConfig {

        private Integer corePoolSize;
        private Integer maximumPoolSize;
        private Integer queueCapacity;
        private Long keepAliveSeconds;
        private Long awaitTerminationSeconds;
        private Boolean allowCoreThreadTimeout;

        static PoolConfig sdkDefaults() {
            int processors = Runtime.getRuntime().availableProcessors();
            PoolConfig config = new PoolConfig();
            config.corePoolSize = Math.max(1, processors);
            config.maximumPoolSize = Math.max(2, processors * 2);
            config.queueCapacity = 1000;
            config.keepAliveSeconds = 60L;
            config.awaitTerminationSeconds = 30L;
            config.allowCoreThreadTimeout = false;
            return config;
        }

        public Integer getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(Integer corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public Integer getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(Integer maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public Integer getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(Integer queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public Long getKeepAliveSeconds() {
            return keepAliveSeconds;
        }

        public void setKeepAliveSeconds(Long keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }

        public Long getAwaitTerminationSeconds() {
            return awaitTerminationSeconds;
        }

        public void setAwaitTerminationSeconds(Long awaitTerminationSeconds) {
            this.awaitTerminationSeconds = awaitTerminationSeconds;
        }

        public Boolean getAllowCoreThreadTimeout() {
            return allowCoreThreadTimeout;
        }

        public void setAllowCoreThreadTimeout(Boolean allowCoreThreadTimeout) {
            this.allowCoreThreadTimeout = allowCoreThreadTimeout;
        }
    }
}
