package org.example.study.text.cache;

import org.example.study.domain.DetectAction;
import org.example.study.text.config.TextViolationCacheProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证违规缓存只保存哈希键和最小结论。 */
class RedisTextViolationCacheTests {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final TextViolationCacheProperties properties = properties();
    private final RedisTextViolationCache cache = new RedisTextViolationCache(
            redisTemplate, new ObjectMapper(), properties);

    @Test
    void storesRejectedResultWithoutRawIdentifiersOrText() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        CachedTextViolation violation = new CachedTextViolation(
                DetectAction.REJECT, List.of("keyword_risk"), "v1", Instant.parse("2026-01-01T00:00:00Z"));

        cache.store("raw-app-id", "raw-scene", "content-sha256", violation);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(Duration.ofDays(7)));
        assertThat(keyCaptor.getValue())
                .doesNotContain("raw-app-id", "raw-scene")
                .endsWith(":content-sha256");
        assertThat(valueCaptor.getValue())
                .contains("keyword_risk")
                .doesNotContain("raw-app-id", "raw-scene");
    }

    @Test
    void doesNotStoreReviewResult() {
        CachedTextViolation violation = new CachedTextViolation(
                DetectAction.REVIEW, List.of("contact_risk"), "v1", Instant.now());

        cache.store("app", "scene", "hash", violation);

        verify(redisTemplate, never()).opsForValue();
    }

    private TextViolationCacheProperties properties() {
        TextViolationCacheProperties result = new TextViolationCacheProperties();
        result.setEnabled(true);
        result.setTtl(Duration.ofDays(7));
        return result;
    }
}
