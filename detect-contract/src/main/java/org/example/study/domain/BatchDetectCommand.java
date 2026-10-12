package org.example.study.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/** 整批共享编号，RPC 和内部消息均只传元数据及文件引用。 */
public record BatchDetectCommand(String taskId, String clientRequestId, String traceId, String appId,
                                 String sceneCode, ContentType contentType, String language, String region,
                                 DetectExecutionMode executionMode, Instant submittedAt,
                                 List<BatchDetectItem> items) implements Serializable {
    public BatchDetectCommand {
        items = List.copyOf(items);
        if (items.isEmpty() || items.size() > 100 || contentType == null || contentType == ContentType.MULTIMODAL
                || executionMode == null || taskId == null || appId == null) {
            throw new IllegalArgumentException("Invalid batch command envelope");
        }
        var identifiers = new java.util.HashSet<String>();
        for (var item : items) {
            if (item.itemId() == null || !identifiers.add(item.itemId())) {
                throw new IllegalArgumentException("Batch item identifiers must be unique");
            }
        }
    }
}
