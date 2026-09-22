package org.example.study.baseSdk.database.api.model;

import java.time.LocalDateTime;

/**
 * 数据库 SDK 对外暴露的通用配置数据，不包含 MyBatis 注解。
 */
public record ConfigItemData(
        Long id,
        String namespace,
        String configKey,
        String configValue,
        Long version,
        boolean enabled,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
