package org.example.study.baseSdk.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 基于 SLF4J 的默认结构化日志记录器。
 */
@Component
public class Slf4jLogRecorder implements LogSink {

    private static final Logger log = LoggerFactory.getLogger(Slf4jLogRecorder.class);

    @Override
    public void record(BaseLogEvent event) {
        String logMessage = "base_log logSpace={} logType={} traceId={} taskId={} appId={} sceneCode={} source={} eventType={} success={} costMillis={} message={} attributes={}";
        Object[] args = {
                event.logSpace(),
                event.logType(),
                event.traceId(),
                event.taskId(),
                event.appId(),
                event.sceneCode(),
                event.source(),
                event.eventType(),
                event.success(),
                event.costMillis(),
                event.message(),
                event.attributes()
        };

        switch (event.logLevel()) {
            case TRACE -> log.trace(logMessage, args);
            case DEBUG -> log.debug(logMessage, args);
            case WARN -> log.warn(logMessage, args);
            case ERROR -> log.error(logMessage, args);
            default -> log.info(logMessage, args);
        }
    }
}
