package org.example.study.baseSdk.hotload;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 热加载调度参数；业务配置本身不放在此处。
 */
@ConfigurationProperties(prefix = "base-sdk.hotload")
public class HotLoadProperties {

    private long fullRefreshIntervalMillis = 3_600_000;
    private int batchSize = 500;

    public long getFullRefreshIntervalMillis() {
        return fullRefreshIntervalMillis;
    }

    public void setFullRefreshIntervalMillis(long fullRefreshIntervalMillis) {
        this.fullRefreshIntervalMillis = fullRefreshIntervalMillis;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
