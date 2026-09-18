package org.example.study.exception;

/**
 * 异步检测任务未能可靠写入消息队列时抛出的异常。
 */
public class DetectTaskPublishException extends RuntimeException {

    public DetectTaskPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
