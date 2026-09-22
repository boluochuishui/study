package org.example.study.baseSdk.concurrent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ManagedThreadPoolFactoryTests {

    private ManagedThreadPoolFactory factory;

    @AfterEach
    void tearDown() throws Exception {
        MDC.clear();
        if (factory != null) {
            factory.destroy();
        }
    }

    @Test
    void shouldCreateConfiguredPoolAndReuseSameInstance() {
        ThreadPoolProperties properties = new ThreadPoolProperties();
        ThreadPoolProperties.PoolConfig config = new ThreadPoolProperties.PoolConfig();
        config.setCorePoolSize(2);
        config.setMaximumPoolSize(3);
        config.setQueueCapacity(7);
        properties.setPools(Map.of("detect-async", config));
        factory = new ManagedThreadPoolFactory(properties);

        ThreadPoolExecutor first = factory.create("detect-async");
        ThreadPoolExecutor second = factory.create("detect-async");

        assertThat(second).isSameAs(first);
        assertThat(first.getCorePoolSize()).isEqualTo(2);
        assertThat(first.getMaximumPoolSize()).isEqualTo(3);
        assertThat(first.getQueue().remainingCapacity()).isEqualTo(7);
    }

    @Test
    void shouldTransferMdcAndUseNamedThread() throws InterruptedException {
        factory = new ManagedThreadPoolFactory(new ThreadPoolProperties());
        ThreadPoolExecutor executor = factory.create("context-test");
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<String> traceId = new AtomicReference<>();
        AtomicReference<String> threadName = new AtomicReference<>();
        MDC.put("traceId", "trace-001");

        executor.execute(() -> {
            traceId.set(MDC.get("traceId"));
            threadName.set(Thread.currentThread().getName());
            completed.countDown();
        });

        assertThat(completed.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(traceId).hasValue("trace-001");
        assertThat(threadName.get()).startsWith("context-test-");
    }

    @Test
    void shouldRejectTaskAfterShutdown() {
        factory = new ManagedThreadPoolFactory(new ThreadPoolProperties());
        ThreadPoolExecutor executor = factory.create("shutdown-test");
        executor.shutdown();

        assertThatThrownBy(() -> executor.execute(() -> { }))
                .isInstanceOf(RejectedExecutionException.class)
                .hasMessageContaining("shutdown-test");
    }

    @Test
    void shouldRejectInvalidDefinition() {
        ThreadPoolProperties properties = new ThreadPoolProperties();
        ThreadPoolProperties.PoolConfig config = new ThreadPoolProperties.PoolConfig();
        config.setCorePoolSize(4);
        config.setMaximumPoolSize(2);
        properties.setPools(Map.of("invalid", config));
        assertThatThrownBy(() -> new ManagedThreadPoolFactory(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Maximum pool size must not be less than core pool size");
    }
}
