package org.example.study.admin.api;

import java.time.Instant;

/**
 * 管理台统一响应。
 */
public record AdminResponse<T>(String code, String message, T data, Instant timestamp) {
}
