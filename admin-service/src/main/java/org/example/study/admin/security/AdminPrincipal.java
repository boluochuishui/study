package org.example.study.admin.security;

import java.util.Set;

/** 当前管理台请求的租户身份。 */
public record AdminPrincipal(long userId, long tenantId, String tenantCode, String username,
                             String displayName, String tokenHash, Set<String> permissions) {
    public AdminPrincipal {
        permissions = Set.copyOf(permissions);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
