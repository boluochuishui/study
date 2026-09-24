package org.example.study.admin.api;

import java.time.LocalDateTime;
import java.util.Set;

/** 登录成功后返回的不透明访问令牌和身份摘要。 */
public record AdminLoginResponse(String accessToken, LocalDateTime expiresAt, long tenantId,
                                 String tenantCode, long userId, String username, Set<String> permissions) {
    public AdminLoginResponse {
        permissions = Set.copyOf(permissions);
    }
}
