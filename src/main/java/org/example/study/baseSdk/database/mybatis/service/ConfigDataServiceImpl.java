package org.example.study.baseSdk.database.mybatis.service;

import org.example.study.baseSdk.database.api.exception.DatabaseSdkException;
import org.example.study.baseSdk.database.api.model.ConfigItemData;
import org.example.study.baseSdk.database.api.model.SaveConfigItemCommand;
import org.example.study.baseSdk.database.api.service.ConfigDataService;
import org.example.study.baseSdk.database.mybatis.entity.ConfigItemEntity;
import org.example.study.baseSdk.database.mybatis.mapper.ConfigItemMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis-Plus 的配置数据服务实现。
 */
@Service
public class ConfigDataServiceImpl implements ConfigDataService {

    private final ConfigItemMapper configItemMapper;

    public ConfigDataServiceImpl(ConfigItemMapper configItemMapper) {
        this.configItemMapper = configItemMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfigItemData> findEnabled(String namespace, String configKey) {
        validateKey(namespace, configKey);
        try {
            return Optional.ofNullable(configItemMapper.selectEnabled(namespace, configKey))
                    .map(this::toData);
        } catch (DataAccessException exception) {
            throw new DatabaseSdkException("Failed to query config item", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfigItemData> listEnabled(String namespace) {
        requireText(namespace, "Config namespace must not be blank");
        try {
            return configItemMapper.selectEnabledList(namespace).stream()
                    .map(this::toData)
                    .toList();
        } catch (DataAccessException exception) {
            throw new DatabaseSdkException("Failed to query config items", exception);
        }
    }

    @Override
    @Transactional
    public ConfigItemData save(SaveConfigItemCommand command) {
        validateCommand(command);
        try {
            ConfigItemEntity entity = configItemMapper.selectAny(command.namespace(), command.configKey());
            if (entity == null) {
                entity = new ConfigItemEntity();
                entity.setNamespace(command.namespace());
                entity.setConfigKey(command.configKey());
                entity.setVersion(1L);
                entity.setDeleted(0);
            }
            entity.setConfigValue(command.configValue());
            entity.setEnabled(command.enabled());
            entity.setDescription(command.description());
            int affectedRows = entity.getId() == null
                    ? configItemMapper.insert(entity)
                    : configItemMapper.updateById(entity);
            if (affectedRows != 1) {
                throw new DatabaseSdkException("Config item was modified concurrently");
            }
            return toData(entity);
        } catch (DatabaseSdkException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw new DatabaseSdkException("Failed to save config item", exception);
        }
    }

    @Override
    @Transactional
    public boolean disable(String namespace, String configKey) {
        validateKey(namespace, configKey);
        try {
            ConfigItemEntity entity = configItemMapper.selectAny(namespace, configKey);
            if (entity == null || Boolean.FALSE.equals(entity.getEnabled())) {
                return false;
            }
            entity.setEnabled(false);
            if (configItemMapper.updateById(entity) != 1) {
                throw new DatabaseSdkException("Config item was modified concurrently");
            }
            return true;
        } catch (DatabaseSdkException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw new DatabaseSdkException("Failed to disable config item", exception);
        }
    }

    private ConfigItemData toData(ConfigItemEntity entity) {
        return new ConfigItemData(
                entity.getId(), entity.getNamespace(), entity.getConfigKey(), entity.getConfigValue(),
                entity.getVersion(), Boolean.TRUE.equals(entity.getEnabled()), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    private void validateCommand(SaveConfigItemCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Save config command must not be null");
        }
        validateKey(command.namespace(), command.configKey());
        requireText(command.configValue(), "Config value must not be blank");
    }

    private void validateKey(String namespace, String configKey) {
        requireText(namespace, "Config namespace must not be blank");
        requireText(configKey, "Config key must not be blank");
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
