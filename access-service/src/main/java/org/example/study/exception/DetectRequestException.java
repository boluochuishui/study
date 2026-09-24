package org.example.study.exception;

/**
 * 检测请求不符合接口约束时抛出的异常。
 */
public class DetectRequestException extends RuntimeException {

    private final String errorCode;

    public DetectRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
