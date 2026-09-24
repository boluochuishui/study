package org.example.study.domain;

import java.time.Instant;
import java.io.Serializable;

/**
 * 接入服务发送给检测服务的稳定 RPC 命令。
 */
public record DetectCommand(
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
) implements Serializable {
}
