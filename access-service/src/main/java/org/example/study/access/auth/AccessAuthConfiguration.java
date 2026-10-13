package org.example.study.access.auth;

import org.example.study.baseSdk.auth.AuthConfigSnapshot;
import org.example.study.baseSdk.auth.AuthMode;
import org.example.study.baseSdk.auth.AuthRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 接入服务提供自身的引导鉴权规则；发布快照由数据库适配器读取。 */
@Configuration(proxyBeanMethods = false)
public class AccessAuthConfiguration {

    @Bean
    public AuthConfigSnapshot bootstrapAuthSnapshot() {
        return new AuthConfigSnapshot(
                "bootstrap-v1", Instant.EPOCH, "system",
                List.of(
                        new AuthRule("detect-sync", "同步检测", "/api/detect/sync", Set.of("POST"),
                                AuthMode.SIGNATURE, Map.of("requiredPermission", "detect:sync"), 10, true),
                        new AuthRule("detect-async", "异步检测", "/api/detect/async", Set.of("POST"),
                                AuthMode.SIGNATURE, Map.of("requiredPermission", "detect:async"), 20, true),
                        new AuthRule("health-liveness", "存活检查", "/actuator/health/liveness", Set.of("GET"),
                                AuthMode.ANONYMOUS, Map.of(), 100, true),
                        new AuthRule("health-readiness", "就绪检查", "/actuator/health/readiness", Set.of("GET"),
                                AuthMode.ANONYMOUS, Map.of(), 110, true)
                ), "bootstrap");
    }
}
