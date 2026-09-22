package org.example.study.baseSdk.database.api.exception;

/**
 * 配置项创建或更新时发生版本冲突。
 */
public class ConfigConflictException extends DatabaseSdkException {

    public ConfigConflictException() {
        super("Config item was modified concurrently");
    }

    public ConfigConflictException(Throwable cause) {
        super("Config item was modified concurrently", cause);
    }
}
