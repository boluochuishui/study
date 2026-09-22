package org.example.study.baseSdk.log;

/**
 * 日志输出端扩展点，可以按日志空间或日志类型扩展不同落地方式。
 */
public interface LogSink {

    default boolean supports(BaseLogEvent event) {
        return true;
    }

    void record(BaseLogEvent event);
}
