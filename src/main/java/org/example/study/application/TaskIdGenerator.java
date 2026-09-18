package org.example.study.application;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 生成服务端控制的任务、事件和链路标识。
 */
@Component
public class TaskIdGenerator {

    public String nextTaskId() {
        return "dt_" + compactUuid();
    }

    public String nextEventId() {
        return "evt_" + compactUuid();
    }

    public String nextTraceId() {
        return "trace_" + compactUuid();
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
