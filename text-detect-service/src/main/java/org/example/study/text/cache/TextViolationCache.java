package org.example.study.text.cache;

import java.util.Optional;

/** 文本违规样本缓存抽象，业务节点不依赖具体 Redis API。 */
public interface TextViolationCache {

    Optional<CachedTextViolation> find(String appId, String sceneCode, String contentHash);

    void store(String appId, String sceneCode, String contentHash, CachedTextViolation violation);
}
