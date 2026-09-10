package org.example.study.baseSdk.log;

import org.springframework.stereotype.Component;

/**
 * 默认业务日志门面，底层委托给通用日志记录器。
 */
@Component
public class DefaultBizLogRecorder implements BizLogRecorder {

    private final LogRecorder logRecorder;

    public DefaultBizLogRecorder(LogRecorder logRecorder) {
        this.logRecorder = logRecorder;
    }

    @Override
    public void record(BizLogEvent event) {
        logRecorder.record(event.toBaseLogEvent());
    }
}
