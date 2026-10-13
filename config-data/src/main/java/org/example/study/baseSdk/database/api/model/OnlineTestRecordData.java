package org.example.study.baseSdk.database.api.model;

import java.time.LocalDateTime;

/** 在线测试记录只包含元数据与检测响应，不包含原始样本。 */
public record OnlineTestRecordData(Long id, long tenantId, long userId, String contentType,
                                   String executionMode, String appId, String sceneCode, int itemCount,
                                   String taskId, String status, String resultJson, long durationMs,
                                   LocalDateTime createdAt) {
}
