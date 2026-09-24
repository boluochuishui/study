package org.example.study.baseSdk.auth;

/**
 * 单一鉴权模式的扩展点。
 */
public interface AuthenticationProvider {

    AuthMode supportMode();

    AuthenticationPrincipal authenticate(AuthenticationRequest request, AuthRule rule);
}
