package org.example.study.baseSdk.database.api.model;

import java.util.Set;

/** 访问令牌对应的实时身份及权限集合。 */
public record AdminIdentityData(long userId, long tenantId, String tenantCode, String username,
                                String displayName, Set<String> permissions) {
    public AdminIdentityData {
        permissions = Set.copyOf(permissions);
    }
}
