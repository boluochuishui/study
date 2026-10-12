package org.example.study.text.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 文本大模型检测配置。 */
@Component
@ConfigurationProperties(prefix = "detect.text.large-model")
public class TextLargeModelProperties {

    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
