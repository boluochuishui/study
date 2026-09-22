package org.example.study.baseSdk.concurrent;

/**
 * 线程池最终生效的不可变配置。
 */
public record ThreadPoolDefinition(
        int corePoolSize,
        int maximumPoolSize,
        int queueCapacity,
        long keepAliveSeconds,
        long awaitTerminationSeconds,
        boolean allowCoreThreadTimeout
) {

    public ThreadPoolDefinition {
        if (corePoolSize <= 0) {
            throw new IllegalArgumentException("Core pool size must be greater than zero");
        }
        if (maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("Maximum pool size must not be less than core pool size");
        }
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("Queue capacity must be greater than zero");
        }
        if (keepAliveSeconds < 0) {
            throw new IllegalArgumentException("Keep alive seconds must not be negative");
        }
        if (awaitTerminationSeconds < 0) {
            throw new IllegalArgumentException("Await termination seconds must not be negative");
        }
    }
}
