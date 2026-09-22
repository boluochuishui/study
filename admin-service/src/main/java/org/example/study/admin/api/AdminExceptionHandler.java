package org.example.study.admin.api;

import jakarta.servlet.http.HttpServletRequest;
import org.example.study.baseSdk.database.api.exception.DatabaseSdkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * 错误码稳定，展示消息由语言资源决定。
 */
@RestControllerAdvice
public class AdminExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminExceptionHandler.class);

    private final MessageSource messages;

    public AdminExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(AdminApiException.class)
    public ResponseEntity<AdminResponse<Void>> handleAdmin(AdminApiException exception, HttpServletRequest request) {
        return error(exception.status(), exception.code(), request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<AdminResponse<Void>> handleInvalidRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", request);
    }

    @ExceptionHandler(DatabaseSdkException.class)
    public ResponseEntity<AdminResponse<Void>> handleDatabase(DatabaseSdkException exception, HttpServletRequest request) {
        LOGGER.error("Admin database operation failed", exception);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AdminResponse<Void>> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Admin request failed", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", request);
    }

    private ResponseEntity<AdminResponse<Void>> error(HttpStatus status, String code, HttpServletRequest request) {
        String message = messages.getMessage("admin.error." + code, null, AdminLocale.resolve(request));
        return ResponseEntity.status(status).body(new AdminResponse<>(code, message, null, Instant.now()));
    }
}
