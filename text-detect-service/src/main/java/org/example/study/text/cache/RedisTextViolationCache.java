package org.example.study.text.cache;

import org.example.study.domain.DetectAction;
import org.example.study.text.config.TextViolationCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 基于 Redis 的违规样本缓存，键和值都不包含文本原文。
 */
@Component
public class RedisTextViolationCache implements TextViolationCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTextViolationCache.class);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String keyPrefix;
    private final String decisionVersion;
    private final Duration ttl;

    public RedisTextViolationCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                   TextViolationCacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.enabled = properties.isEnabled();
        this.keyPrefix = properties.getKeyPrefix();
        this.decisionVersion = properties.getDecisionVersion();
        this.ttl = properties.getTtl();
    }

    @Override
    public Optional<CachedTextViolation> find(String appId, String sceneCode, String contentHash) {
        if (!enabled) {
            return Optional.empty();
        }
        String cacheKey = cacheKey(appId, sceneCode, contentHash);
        try {
            String value = redisTemplate.opsForValue().get(cacheKey);
            if (value == null) {
                return Optional.empty();
            }
            CachedTextViolation violation = objectMapper.readValue(value, CachedTextViolation.class);
            if (violation.action() != DetectAction.REJECT) {
                LOGGER.warn("Ignored invalid text violation cache entry, cacheKey={}", cacheKey);
                return Optional.empty();
            }
            return Optional.of(violation);
        } catch (Exception exception) {
            LOGGER.error("Text violation cache lookup failed, cacheKey={}", cacheKey, exception);
            return Optional.empty();
        }
    }

    @Override
    public void store(String appId, String sceneCode, String contentHash, CachedTextViolation violation) {
        if (!enabled || violation.action() != DetectAction.REJECT) {
            return;
        }
        String cacheKey = cacheKey(appId, sceneCode, contentHash);
        try {
            String value = objectMapper.writeValueAsString(violation);
            redisTemplate.opsForValue().set(cacheKey, value, ttl);
        } catch (Exception exception) {
            LOGGER.error("Text violation cache write failed, cacheKey={}", cacheKey, exception);
        }
    }

    String cacheKey(String appId, String sceneCode, String contentHash) {
        return String.join(":", keyPrefix, decisionVersion,
                sha256(appId), sha256(sceneCode), contentHash);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}
