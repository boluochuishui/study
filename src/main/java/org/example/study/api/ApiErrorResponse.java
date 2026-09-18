package org.example.study.api;

import java.time.Instant;

/**
 * 对外统一错误响应，不暴露内部异常堆栈。
 */
public record ApiErrorResponse(
        String code,
        String message,
        Instant occurredAt
) {
}
