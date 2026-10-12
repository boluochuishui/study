package org.example.study.api;

import org.example.study.exception.DetectRequestException;
import org.example.study.exception.DetectTaskPublishException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * 将可预期异常转换为稳定的英文错误响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadSize(org.springframework.web.multipart.MaxUploadSizeExceededException exception) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "REQUEST_BODY_TOO_LARGE", "Upload size limit exceeded");
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    public ResponseEntity<ApiErrorResponse> handleInvalidBody(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request body or metadata is invalid");
    }

    @ExceptionHandler(DetectRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleDetectRequest(DetectRequestException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.errorCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request validation failed");
    }

    @ExceptionHandler(DetectTaskPublishException.class)
    public ResponseEntity<ApiErrorResponse> handlePublishFailure(DetectTaskPublishException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "TASK_PUBLISH_FAILED", "Failed to publish detection task");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message, Instant.now()));
    }
}
