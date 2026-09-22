package org.example.study.baseSdk.log;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 统一日志记录入口，负责分发到匹配的日志输出端。
 */
@Primary
@Component
public class CompositeLogRecorder implements LogRecorder {

    private final List<LogSink> logSinks;

    public CompositeLogRecorder(List<LogSink> logSinks) {
        this.logSinks = List.copyOf(logSinks);
    }

    @Override
    public void record(BaseLogEvent event) {
        logSinks.stream()
                .filter(logSink -> logSink.supports(event))
                .forEach(logSink -> logSink.record(event));
    }
}
