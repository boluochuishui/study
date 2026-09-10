package org.example.study.baseSdk.log;

/**
 * 通用日志记录入口。
 */
public interface LogRecorder {

    void record(BaseLogEvent event);
}
