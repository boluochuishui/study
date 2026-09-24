package org.example.study.baseSdk.auth;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 单实例防重放实现，仅用于当前开发阶段。
 */
@Component
public class InMemoryNonceStore implements NonceStore {

    private final ConcurrentMap<String, Long> nonces = new ConcurrentHashMap<>();

    @Override
    public boolean acquire(String credentialId, String nonce, long expireEpochSecond) {
        long now = Instant.now().getEpochSecond();
        nonces.entrySet().removeIf(entry -> entry.getValue() <= now);
        String key = credentialId + ':' + nonce;
        return nonces.putIfAbsent(key, expireEpochSecond) == null;
    }
}
