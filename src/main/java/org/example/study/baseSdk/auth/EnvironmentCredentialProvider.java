package org.example.study.baseSdk.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 个人开发环境的凭证源，密钥由环境变量注入且不写入数据库。
 */
@Component
public class EnvironmentCredentialProvider implements CredentialProvider {

    private final String appId;
    private final String appSecret;

    public EnvironmentCredentialProvider(
            @Value("${study.auth.app-id}") String appId,
            @Value("${study.auth.app-secret}") String appSecret
    ) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    @Override
    public Optional<Credential> load(String credentialId) {
        if (!appId.equals(credentialId)) {
            return Optional.empty();
        }
        return Optional.of(new Credential(appId, appSecret, true, null));
    }
}
