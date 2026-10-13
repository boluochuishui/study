package org.example.study.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/** 请求共享编号，RPC 和内部消息均只传元数据及文件引用。 */
public record DetectCommand(String taskId, String clientRequestId, String traceId, String appId,
                            String sceneCode, ContentType contentType, String language, String region,
                            DetectExecutionMode executionMode, Instant submittedAt,
                            List<DetectItem> items) implements Serializable {
    public DetectCommand {
        items = List.copyOf(items);
        if (items.isEmpty() || items.size() > 100 || contentType == null || contentType == ContentType.MULTIMODAL
                || executionMode == null || taskId == null || appId == null) {
            throw new IllegalArgumentException("Invalid detection command envelope");
        }
        if ((executionMode == DetectExecutionMode.SYNC || contentType == ContentType.AUDIO
                || contentType == ContentType.VIDEO) && items.size() != 1) {
            throw new IllegalArgumentException("Detection command requires exactly one item");
        }
        if (executionMode == DetectExecutionMode.SYNC && contentType != ContentType.TEXT && contentType != ContentType.IMAGE) {
            throw new IllegalArgumentException("Synchronous media detection is not supported");
        }
        var identifiers = new java.util.HashSet<String>();
        for (var item : items) {
            if (item.itemId() == null || !identifiers.add(item.itemId())) {
                throw new IllegalArgumentException("Detection item identifiers must be unique");
            }
        }
    }
}
