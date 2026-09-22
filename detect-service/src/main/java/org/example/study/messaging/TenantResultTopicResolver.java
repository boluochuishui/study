package org.example.study.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 将 appId 映射为不泄露原值且符合命名约束的租户独立 Topic。
 */
@Component
public class TenantResultTopicResolver {

    private final String topicPrefix;

    public TenantResultTopicResolver(
            @Value("${study.detect.async.rocketmq.result-topic-prefix:detect-result-}") String topicPrefix
    ) {
        this.topicPrefix = topicPrefix;
    }

    public String resolve(String appId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(appId.getBytes(StandardCharsets.UTF_8));
            return topicPrefix + HexFormat.of().formatHex(digest, 0, 8);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to resolve tenant result topic", exception);
        }
    }
}
