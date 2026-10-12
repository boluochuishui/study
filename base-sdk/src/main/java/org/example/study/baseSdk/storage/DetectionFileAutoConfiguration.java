package org.example.study.baseSdk.storage;

import java.io.IOException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** 文件存储的默认实现可由业务替换。 */
@AutoConfiguration
@EnableConfigurationProperties(DetectionFileProperties.class)
public class DetectionFileAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(DetectionFileStore.class)
    public SharedDetectionFileStore detectionFileStore(DetectionFileProperties properties) throws IOException {
        return new SharedDetectionFileStore(properties);
    }
}
