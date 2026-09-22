package org.example.study.baseSdk.concurrent;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;

/**
 * 在任务提交时捕获 MDC，并在工作线程执行结束后清理上下文。
 */
final class ContextAwareThreadPoolExecutor extends ThreadPoolExecutor {

    ContextAwareThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory,
            RejectedExecutionHandler handler
    ) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public void execute(Runnable command) {
        Map<String, String> callerContext = MDC.getCopyOfContextMap();
        super.execute(() -> {
            Map<String, String> workerContext = MDC.getCopyOfContextMap();
            try {
                restore(callerContext);
                command.run();
            } finally {
                restore(workerContext);
            }
        });
    }

    private static void restore(Map<String, String> context) {
        MDC.clear();
        if (context != null && !context.isEmpty()) {
            MDC.setContextMap(context);
        }
    }
}
