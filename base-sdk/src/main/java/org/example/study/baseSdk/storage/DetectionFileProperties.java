package org.example.study.baseSdk.storage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 所有接入与 Worker 必须指向同一共享目录，保留时间覆盖异步任务及重投递。 */
@ConfigurationProperties("base-sdk.detection-files")
public class DetectionFileProperties {
    private String root = System.getProperty("java.io.tmpdir") + "/study-detect-files";
    private Duration retention = Duration.ofHours(24);

    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }
    public Duration getRetention() { return retention; }
    public void setRetention(Duration retention) { this.retention = retention; }
}
