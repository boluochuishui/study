package org.example.study.baseSdk.database.api.service;

import org.example.study.baseSdk.database.api.model.ConfigItemData;
import org.example.study.baseSdk.database.api.model.SaveConfigItemCommand;

import java.util.List;
import java.util.Optional;

/**
 * 数据库 SDK 对外提供的配置数据服务，调用方不直接依赖 Mapper。
 */
public interface ConfigDataService {

    Optional<ConfigItemData> findEnabled(long tenantId, String namespace, String configKey);

    List<ConfigItemData> listEnabled(long tenantId, String namespace);

    Optional<ConfigItemData> find(long tenantId, String namespace, String configKey);

    List<ConfigItemData> list(long tenantId, String namespace);

    ConfigItemData save(SaveConfigItemCommand command);

    ConfigItemData saveIfVersion(SaveConfigItemCommand command, long expectedVersion);

    boolean disable(long tenantId, String namespace, String configKey);
}
