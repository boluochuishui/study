package org.example.study.domain;

import java.io.Serializable;
import java.util.List;

/** 逐项公开结果，不包含内部责任链轨迹和存储引用。 */
public record DetectItemResult(String itemId, DetectStatus detectStatus, DetectAction action,
                               List<String> labels, boolean degraded, String errorCode,
                               String errorMessage) implements Serializable {
    public DetectItemResult {
        labels = List.copyOf(labels);
    }

    public static DetectItemResult from(String itemId, DetectResult result) {
        return new DetectItemResult(itemId, result.detectStatus(), result.action(), result.labels(),
                result.degraded(), result.errorCode(), result.errorMessage());
    }
}
