package org.example.study.domain;

import jakarta.validation.constraints.NotBlank;

/**
 * 统一检测请求。任务 ID 由服务端生成，clientRequestId 仅用于关联客户业务。
 */
public record DetectRequest(
        String clientRequestId,
        @NotBlank String sceneCode,
        @NotBlank String contentType,
        String text,
        String imageUrl,
        String audioUrl,
        String videoUrl
) {
}
