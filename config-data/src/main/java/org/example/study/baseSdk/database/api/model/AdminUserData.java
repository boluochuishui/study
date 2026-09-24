package org.example.study.baseSdk.database.api.model;

/** 管理台用户及所属租户数据。 */
public record AdminUserData(Long id, long tenantId, String tenantCode, String username,
                            String displayName, String passwordHash, boolean enabled) {
}
