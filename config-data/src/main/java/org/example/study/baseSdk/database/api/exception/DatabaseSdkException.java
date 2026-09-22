package org.example.study.baseSdk.database.api.exception;

/**
 * 数据库 SDK 对底层数据库异常的统一封装。
 */
public class DatabaseSdkException extends RuntimeException {

    public DatabaseSdkException(String message) {
        super(message);
    }

    public DatabaseSdkException(String message, Throwable cause) {
        super(message, cause);
    }
}
