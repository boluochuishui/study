package org.example.study.baseSdk.auth;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 显式匿名规则的认证实现。
 */
@Component
public class AnonymousAuthenticationProvider implements AuthenticationProvider {

    @Override
    public AuthMode supportMode() {
        return AuthMode.ANONYMOUS;
    }

    @Override
    public AuthenticationPrincipal authenticate(AuthenticationRequest request, AuthRule rule) {
        return new AuthenticationPrincipal("anonymous", AuthMode.ANONYMOUS, "", Set.of());
    }
}
