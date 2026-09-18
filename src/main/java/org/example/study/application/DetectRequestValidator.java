package org.example.study.application;

import org.example.study.domain.ContentType;
import org.example.study.domain.DetectExecutionMode;
import org.example.study.domain.DetectRequest;
import org.example.study.exception.DetectRequestException;
import org.springframework.stereotype.Component;

/**
 * 集中校验执行模式、内容类型和对应载荷。
 */
@Component
public class DetectRequestValidator {

    public ContentType validate(DetectRequest request, DetectExecutionMode executionMode) {
        ContentType contentType = ContentType.from(request.contentType());
        validateExecutionMode(contentType, executionMode);
        validatePayload(contentType, request);
        return contentType;
    }

    private void validateExecutionMode(ContentType contentType, DetectExecutionMode executionMode) {
        boolean supported = switch (executionMode) {
            case SYNC -> contentType == ContentType.TEXT || contentType == ContentType.IMAGE;
            case ASYNC -> contentType == ContentType.TEXT
                    || contentType == ContentType.IMAGE
                    || contentType == ContentType.AUDIO
                    || contentType == ContentType.VIDEO;
        };
        if (!supported) {
            throw new DetectRequestException(
                    "UNSUPPORTED_EXECUTION_MODE",
                    "Content type " + contentType + " is not supported in " + executionMode.name().toLowerCase() + " detection"
            );
        }
    }

    private void validatePayload(ContentType contentType, DetectRequest request) {
        boolean missing = switch (contentType) {
            case TEXT -> isBlank(request.text());
            case IMAGE -> isBlank(request.imageUrl());
            case AUDIO -> isBlank(request.audioUrl());
            case VIDEO -> isBlank(request.videoUrl());
            case MULTIMODAL -> true;
        };
        if (missing) {
            throw new DetectRequestException("MISSING_CONTENT_PAYLOAD", "Missing content payload for " + contentType);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
