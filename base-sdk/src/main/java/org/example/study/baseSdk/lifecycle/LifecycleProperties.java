package org.example.study.baseSdk.lifecycle;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 通用服务上下线配置；下线令牌为空时禁止手工摘流。 */
@ConfigurationProperties(prefix = "base-sdk.lifecycle")
public class LifecycleProperties {
    private Duration drainTimeout = Duration.ofSeconds(40);
    private String drainToken = "";

    public Duration getDrainTimeout() {
        return drainTimeout;
    }

    public void setDrainTimeout(Duration drainTimeout) {
        this.drainTimeout = drainTimeout;
    }

    public String getDrainToken() {
        return drainToken;
    }

    public void setDrainToken(String drainToken) {
        this.drainToken = drainToken;
    }
}
