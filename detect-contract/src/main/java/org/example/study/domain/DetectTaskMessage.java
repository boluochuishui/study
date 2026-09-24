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
        Instant submittedAt
) {
}
