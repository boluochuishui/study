package org.example.study.baseSdk.hotload;

import java.time.Instant;

/**
 * 热加载模块的运行状态，不包含配置内容。
 */
public record HotLoadStatus(
        String moduleName,
        boolean initialized,
        long cursor,
        Instant lastSuccessAt,
        Instant lastFailureAt,
        String lastError
) {
}
