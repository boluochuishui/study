package org.example.study.baseSdk.auth;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 根据规则模式路由到唯一的认证 Provider。
 */
@Component
public class AuthenticationManager {

    private final Map<AuthMode, AuthenticationProvider> providers;

    public AuthenticationManager(List<AuthenticationProvider> providerList) {
        Map<AuthMode, AuthenticationProvider> registered = new EnumMap<>(AuthMode.class);
        for (AuthenticationProvider provider : providerList) {
            if (registered.putIfAbsent(provider.supportMode(), provider) != null) {
                throw new IllegalStateException("Duplicate authentication provider: " + provider.supportMode());
            }
        }
        this.providers = Map.copyOf(registered);
    }

    public AuthenticationPrincipal authenticate(AuthenticationRequest request, AuthRule rule) {
        AuthenticationProvider provider = providers.get(rule.authMode());
        if (provider == null) {
            throw new AuthenticationException("AUTH_MODE_UNSUPPORTED", "Authentication mode is not supported", 403);
        }
        return provider.authenticate(request, rule);
    }
}
