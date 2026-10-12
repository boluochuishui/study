package org.example.study.text.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 文本违规缓存配置。 */
@Component
@ConfigurationProperties(prefix = "detect.text.violation-cache")
public class TextViolationCacheProperties {

    private boolean enabled = true;
    private String keyPrefix = "study:risk:text";
    private String decisionVersion = "v1";
    private Duration ttl = Duration.ofDays(7);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getDecisionVersion() {
        return decisionVersion;
    }

    public void setDecisionVersion(String decisionVersion) {
        this.decisionVersion = decisionVersion;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
