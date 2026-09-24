package org.example.study.domain;

import java.util.List;
import java.io.Serializable;

/**
 * 同步检测的对外结果，不暴露责任链节点等内部执行过程。
 */
public record DetectResult(
        String taskId,
        DetectStatus detectStatus,
        DetectAction action,
        List<String> labels,
        boolean degraded,
        String errorCode,
        String errorMessage
) implements Serializable {

    public DetectResult {
        labels = labels == null ? List.of() : List.copyOf(labels);
        errorCode = errorCode == null ? "" : errorCode;
        errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public static DetectResult degradedPass(String taskId, String errorCode, String errorMessage) {
        return new DetectResult(taskId, DetectStatus.FAILED, DetectAction.PASS, List.of(), true, errorCode, errorMessage);
    }
}
