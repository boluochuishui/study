package org.example.study.baseSdk.database.api.model;

/**
 * 新增或更新通用配置项的命令。
 */
public record SaveConfigItemCommand(
        String namespace,
        String configKey,
        String configValue,
        boolean enabled,
        String description
) {
}
