package org.example.study.baseSdk.log;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 统一日志记录入口，负责分发到匹配的日志输出端。
 */
@Primary
@Component
public class CompositeLogRecorder implements LogRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeLogRecorder.class);

    private final List<LogSink> logSinks;

    public CompositeLogRecorder(List<LogSink> logSinks) {
        this.logSinks = List.copyOf(logSinks);
    }

    @Override
    public void log(BaseLogEvent event) {
        for (LogSink sink : logSinks) {
            try {
                if (sink.supports(event)) {
                    sink.record(event);
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("Log sink failed, sink={}", sink.getClass().getName(), exception);
            }
        }
    }
}
