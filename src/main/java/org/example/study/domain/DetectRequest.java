package org.example.study.domain;

import jakarta.validation.constraints.NotBlank;

public record DetectRequest(
        @NotBlank String taskId,
        @NotBlank String sceneCode,
        @NotBlank String contentType,
        String text,
        String imageUrl,
        String audioUrl,
        String videoUrl
) {
}
