package org.example.study.security;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

/**
 * Demo credential repository. A real service would load this from tenant config.
 */
@Repository
public class ApiCredentialRepository {

    private final Map<String, String> appSecrets = Map.of("demo-app", "demo-secret");

    public Optional<String> findSecretByAppId(String appId) {
        return Optional.ofNullable(appSecrets.get(appId));
    }
}
