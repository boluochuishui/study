package org.example.study.baseSdk.auth;

import java.util.Optional;

/**
 * 凭证来源抽象，便于后续替换为数据库、KMS 或 STS。
 */
public interface CredentialProvider {

    Optional<Credential> load(String credentialId);
}
