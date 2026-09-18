package org.example.study.baseSdk.auth;

/**
 * 防重放存储抽象，多实例部署时可替换为 Redis 原子写入实现。
 */
public interface NonceStore {

    boolean acquire(String credentialId, String nonce, long expireEpochSecond);
}
