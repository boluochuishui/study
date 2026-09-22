package org.example.study.baseSdk.log;

/**
 * Output port for structured business logs.
 */
public interface BizLogRecorder {

    void record(BizLogEvent event);
}
