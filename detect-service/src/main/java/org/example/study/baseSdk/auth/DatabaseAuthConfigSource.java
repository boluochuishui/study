package org.example.study.baseSdk.auth;

import tools.jackson.databind.ObjectMapper;
import org.example.study.baseSdk.database.api.service.ConfigDataService;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 从通用数据库配置 SDK 读取管理台发布的完整规则快照。
 */
@Component
public class DatabaseAuthConfigSource implements AuthConfigSource {

    public static final String NAMESPACE = "system-auth";
    public static final String CONFIG_KEY = "published-rules";

    private final ConfigDataService configDataService;
    private final ObjectMapper objectMapper;

    public DatabaseAuthConfigSource(ConfigDataService configDataService, ObjectMapper objectMapper) {
        this.configDataService = configDataService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<AuthConfigSnapshot> loadLatest() {
        return configDataService.findEnabled(NAMESPACE, CONFIG_KEY).map(item -> {
            try {
                return objectMapper.readValue(item.configValue(), AuthConfigSnapshot.class);
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to parse authentication snapshot", exception);
            }
        });
    }
}
