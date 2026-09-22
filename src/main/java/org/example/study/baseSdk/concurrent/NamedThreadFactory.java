package org.example.study.baseSdk.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 创建可定位来源的工作线程，并统一记录未捕获异常。
 */
final class NamedThreadFactory implements ThreadFactory {

    private static final Logger log = LoggerFactory.getLogger(NamedThreadFactory.class);

    private final String namePrefix;
    private final AtomicInteger sequence = new AtomicInteger();

    NamedThreadFactory(String poolName) {
        this.namePrefix = poolName + "-";
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, namePrefix + sequence.incrementAndGet());
        thread.setDaemon(false);
        thread.setUncaughtExceptionHandler((failedThread, throwable) -> log.error(
                "Uncaught exception in managed thread, threadName={}", failedThread.getName(), throwable));
        return thread;
    }
}
