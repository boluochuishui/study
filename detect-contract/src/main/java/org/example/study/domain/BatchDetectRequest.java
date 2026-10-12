package org.example.study.domain;

import java.util.List;

/** 单请求单模态的批量协议，公共元数据作用于所有项目。 */
public record BatchDetectRequest(String clientRequestId, String sceneCode, String contentType,
                                 String language, String region, List<DetectItemRequest> items) {
    public BatchDetectRequest {
        items = items == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(items));
    }
}
