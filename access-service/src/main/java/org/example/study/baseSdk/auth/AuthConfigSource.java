package org.example.study.baseSdk.auth;

import java.util.Optional;

/**
 * 已发布鉴权配置的读取端口。
 */
public interface AuthConfigSource {

    Optional<AuthConfigSnapshot> loadLatest();
}
