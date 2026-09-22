package org.example.study.admin.service;

import org.example.study.admin.api.AdminApiException;
import org.example.study.admin.api.SaveConfigRequest;
import org.example.study.baseSdk.database.api.exception.ConfigConflictException;
import org.example.study.baseSdk.database.api.model.ConfigItemData;
import org.example.study.baseSdk.database.api.model.SaveConfigItemCommand;
import org.example.study.baseSdk.database.api.service.ConfigDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理台配置操作边界，不向 API 暴露 Mapper。
 */
@Service
public class AdminConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminConfigService.class);

    private final ConfigDataService configDataService;

    public AdminConfigService(ConfigDataService configDataService) {
        this.configDataService = configDataService;
    }

    public List<ConfigItemData> list(String namespace) {
        validateName(namespace);
        return configDataService.list(namespace);
    }

    public ConfigItemData get(String namespace, String key) {
        validateNames(namespace, key);
        return configDataService.find(namespace, key)
                .orElseThrow(() -> new AdminApiException("CONFIG_NOT_FOUND", "Config item not found", HttpStatus.NOT_FOUND));
    }

    public ConfigItemData save(String namespace, String key, SaveConfigRequest request) {
        validateNames(namespace, key);
        if (request == null || request.value() == null || request.value().isBlank()
                || request.enabled() == null || request.version() == null || request.version() < 0
                || request.value().length() > 65535
                || request.description() != null && request.description().length() > 512) {
            throw new AdminApiException("INVALID_REQUEST", "Invalid config request", HttpStatus.BAD_REQUEST);
        }
        try {
            ConfigItemData saved = configDataService.saveIfVersion(new SaveConfigItemCommand(
                    namespace, key, request.value(), request.enabled(), request.description()), request.version());
            LOGGER.info("Admin config saved, namespace={}, key={}, version={}, enabled={}",
                    namespace, key, saved.version(), saved.enabled());
            return saved;
        } catch (ConfigConflictException exception) {
            throw new AdminApiException("CONFIG_CONFLICT", "Config item was modified concurrently", HttpStatus.CONFLICT);
        }
    }

    public ConfigItemData disable(String namespace, String key, Long version) {
        validateNames(namespace, key);
        if (version == null || version < 1) {
            throw new AdminApiException("INVALID_REQUEST", "Invalid config version", HttpStatus.BAD_REQUEST);
        }
        ConfigItemData current = get(namespace, key);
        return save(namespace, key, new SaveConfigRequest(
                current.configValue(), false, current.description(), version));
    }

    private void validateNames(String namespace, String key) {
        validateName(namespace);
        validateName(key);
    }

    private void validateName(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")) {
            throw new AdminApiException("INVALID_REQUEST", "Invalid config identifier", HttpStatus.BAD_REQUEST);
        }
    }
}
