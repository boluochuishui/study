package org.example.study.baseSdk.concurrent;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在线程池拒绝任务时记录关键运行状态，并保持快速失败语义。
 */
final class LoggingAbortPolicy implements RejectedExecutionHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingAbortPolicy.class);

    private final String poolName;

    LoggingAbortPolicy(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        log.error(
                "Managed thread pool rejected task, poolName={}, activeCount={}, poolSize={}, queueSize={}, completedTaskCount={}, shutdown={}",
                poolName,
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount(),
                executor.isShutdown());
        throw new RejectedExecutionException("Managed thread pool rejected task: " + poolName);
    }
}
