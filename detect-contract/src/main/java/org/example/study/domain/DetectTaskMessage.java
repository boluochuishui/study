package org.example.study.domain;

import java.time.Instant;

/**
 * 投递到内部检测任务 Topic 的消息体。
 */
public record DetectTaskMessage(
        String eventId,
        String taskId,
        String clientRequestId,
        String traceId,
        String appId,
        String sceneCode,
        ContentType contentType,
        String text,
        String imageUrl,
        String audioUrl,
        String videoUrl,
        Instant submittedAt,
        BatchDetectCommand batch
) {
    public DetectTaskMessage(String eventId, String taskId, String clientRequestId, String traceId,
                             String appId, String sceneCode, ContentType contentType, String text,
                             String imageUrl, String audioUrl, String videoUrl, Instant submittedAt) {
        this(eventId, taskId, clientRequestId, traceId, appId, sceneCode, contentType,
                text, imageUrl, audioUrl, videoUrl, submittedAt, null);
    }
}
