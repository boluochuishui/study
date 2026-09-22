package org.example.study.baseSdk.concurrent;

/**
 * 线程池运行状态快照。
 */
public record ThreadPoolSnapshot(
        String poolName,
        int poolSize,
        int activeCount,
        int queueSize,
        long completedTaskCount,
        boolean shutdown
) {
}
