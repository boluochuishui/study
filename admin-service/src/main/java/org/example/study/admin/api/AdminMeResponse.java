package org.example.study.admin.api;

import java.util.Set;

/** 当前登录身份。 */
public record AdminMeResponse(long tenantId, String tenantCode, long userId, String username,
                              String displayName, Set<String> permissions) {
    public AdminMeResponse {
        permissions = Set.copyOf(permissions);
    }
}
