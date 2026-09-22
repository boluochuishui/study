package org.example.study.application;

import java.util.concurrent.Executor;

import org.example.study.baseSdk.concurrent.ManagedThreadPoolFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 声明检测业务专用线程池。
 */
@Configuration(proxyBeanMethods = false)
public class DetectAsyncExecutorConfiguration {

    public static final String DETECT_ASYNC_EXECUTOR = "detectAsyncExecutor";

    @Bean(DETECT_ASYNC_EXECUTOR)
    public Executor detectAsyncExecutor(ManagedThreadPoolFactory factory) {
        return factory.create("detect-async");
    }
}
