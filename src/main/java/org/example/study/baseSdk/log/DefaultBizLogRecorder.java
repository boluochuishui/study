package org.example.study.baseSdk.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultBizLogRecorder implements BizLogRecorder {

    private static final Logger log = LoggerFactory.getLogger(DefaultBizLogRecorder.class);

    @Override
    public void record(BizLogEvent event) {
        log.info(
                "biz_event traceId={} taskId={} appId={} sceneCode={} eventType={} success={} costMillis={} message={} attributes={}",
                event.traceId(),
                event.taskId(),
                event.appId(),
                event.sceneCode(),
                event.eventType(),
                event.success(),
                event.costMillis(),
                event.message(),
                event.attributes()
        );
    }
}
