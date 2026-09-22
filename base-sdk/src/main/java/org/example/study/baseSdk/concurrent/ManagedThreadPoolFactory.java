package org.example.study.baseSdk.concurrent;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

/**
 * 根据配置创建并统一管理具名线程池。
 */
public class ManagedThreadPoolFactory implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ManagedThreadPoolFactory.class);

    private final ThreadPoolDefinition defaultDefinition;
    private final Map<String, ThreadPoolDefinition> definitions;
    private final Map<String, ManagedExecutor> executors = new ConcurrentHashMap<>();

    public ManagedThreadPoolFactory(ThreadPoolProperties properties) {
        this.defaultDefinition = resolveDefinition(properties.getDefaults(), null);
        this.definitions = properties.getPools() == null
                ? Map.of()
                : properties.getPools().entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> normalizePoolName(entry.getKey()),
                        entry -> resolveDefinition(properties.getDefaults(), entry.getValue())));
    }

    /**
     * 获取或创建具名线程池，同名调用始终返回同一实例。
     */
    public ThreadPoolExecutor create(String poolName) {
        String normalizedName = normalizePoolName(poolName);
        return executors.computeIfAbsent(normalizedName, this::newExecutor).executor();
    }

    /**
     * 返回线程池当前运行快照，供健康检查和监控适配器使用。
     */
    public ThreadPoolSnapshot snapshot(String poolName) {
        String normalizedName = normalizePoolName(poolName);
        ManagedExecutor managedExecutor = executors.get(normalizedName);
        if (managedExecutor == null) {
            throw new IllegalArgumentException("Managed thread pool does not exist: " + normalizedName);
        }
        ThreadPoolExecutor executor = managedExecutor.executor();
        return new ThreadPoolSnapshot(
                normalizedName,
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount(),
                executor.isShutdown());
    }

    @Override
    public void destroy() {
        executors.values().forEach(this::shutdown);
        executors.clear();
    }

    private ManagedExecutor newExecutor(String poolName) {
        ThreadPoolDefinition definition = definitions.getOrDefault(poolName, defaultDefinition);
        ContextAwareThreadPoolExecutor executor = new ContextAwareThreadPoolExecutor(
                definition.corePoolSize(),
                definition.maximumPoolSize(),
                definition.keepAliveSeconds(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(definition.queueCapacity()),
                new NamedThreadFactory(poolName),
                new LoggingAbortPolicy(poolName));
        executor.allowCoreThreadTimeOut(definition.allowCoreThreadTimeout());
        log.info("Created managed thread pool, poolName={}, definition={}", poolName, definition);
        return new ManagedExecutor(poolName, executor, definition.awaitTerminationSeconds());
    }

    private static ThreadPoolDefinition resolveDefinition(
            ThreadPoolProperties.PoolConfig configuredDefaults,
            ThreadPoolProperties.PoolConfig override
    ) {
        ThreadPoolProperties.PoolConfig sdkDefaults = ThreadPoolProperties.PoolConfig.sdkDefaults();
        ThreadPoolProperties.PoolConfig defaults = Objects.requireNonNullElse(configuredDefaults, sdkDefaults);
        return new ThreadPoolDefinition(
                choose(override == null ? null : override.getCorePoolSize(), choose(defaults.getCorePoolSize(), sdkDefaults.getCorePoolSize())),
                choose(override == null ? null : override.getMaximumPoolSize(), choose(defaults.getMaximumPoolSize(), sdkDefaults.getMaximumPoolSize())),
                choose(override == null ? null : override.getQueueCapacity(), choose(defaults.getQueueCapacity(), sdkDefaults.getQueueCapacity())),
                choose(override == null ? null : override.getKeepAliveSeconds(), choose(defaults.getKeepAliveSeconds(), sdkDefaults.getKeepAliveSeconds())),
                choose(override == null ? null : override.getAwaitTerminationSeconds(), choose(defaults.getAwaitTerminationSeconds(), sdkDefaults.getAwaitTerminationSeconds())),
                choose(override == null ? null : override.getAllowCoreThreadTimeout(), choose(defaults.getAllowCoreThreadTimeout(), sdkDefaults.getAllowCoreThreadTimeout())));
    }

    private void shutdown(ManagedExecutor managedExecutor) {
        ThreadPoolExecutor executor = managedExecutor.executor();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(managedExecutor.awaitTerminationSeconds(), TimeUnit.SECONDS)) {
                int cancelledTasks = executor.shutdownNow().size();
                log.warn("Forced managed thread pool shutdown, poolName={}, cancelledTasks={}", managedExecutor.poolName(), cancelledTasks);
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("Interrupted while shutting down managed thread pool, poolName={}", managedExecutor.poolName(), exception);
        }
    }

    private static String normalizePoolName(String poolName) {
        if (poolName == null || poolName.isBlank()) {
            throw new IllegalArgumentException("Thread pool name must not be blank");
        }
        return poolName.trim();
    }

    private static int choose(Integer override, Integer fallback) {
        return override == null ? Objects.requireNonNull(fallback, "Default integer configuration must not be null") : override;
    }

    private static long choose(Long override, Long fallback) {
        return override == null ? Objects.requireNonNull(fallback, "Default long configuration must not be null") : override;
    }

    private static boolean choose(Boolean override, Boolean fallback) {
        return override == null ? Objects.requireNonNull(fallback, "Default boolean configuration must not be null") : override;
    }

    private record ManagedExecutor(String poolName, ThreadPoolExecutor executor, long awaitTerminationSeconds) {
    }
}
