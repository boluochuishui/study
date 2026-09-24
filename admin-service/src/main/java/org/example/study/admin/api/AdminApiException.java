package org.example.study.admin.api;

import org.springframework.http.HttpStatus;

/**
 * 保留稳定英文异常描述，响应层负责本地化。
 */
public class AdminApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public AdminApiException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public AdminApiException(String code, String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
