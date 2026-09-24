package org.example.study.baseSdk.auth;

import java.util.Set;

/**
 * 认证成功后在当前请求中传递的调用方身份。
 */
public record AuthenticationPrincipal(
        String subject,
        AuthMode authMode,
        String credentialId,
        Set<String> permissions
) {
    public AuthenticationPrincipal {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
