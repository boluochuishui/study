package org.example.study.baseSdk.auth;

/**
 * 可安全转换为外部错误响应的鉴权异常。
 */
public class AuthenticationException extends RuntimeException {

    private final String errorCode;
    private final int status;

    public AuthenticationException(String errorCode, String message, int status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String errorCode() {
        return errorCode;
    }

    public int status() {
        return status;
    }
}
