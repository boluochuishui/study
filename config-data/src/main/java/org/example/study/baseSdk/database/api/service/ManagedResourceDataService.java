package org.example.study.baseSdk.database.api.service;

import java.util.List;
import java.util.Map;
import org.example.study.baseSdk.database.api.model.ManagedResource;

/** 六张业务配置表的租户隔离数据操作。 */
public interface ManagedResourceDataService {
    List<Map<String, Object>> list(long tenantId, ManagedResource resource);

    Map<String, Object> find(long tenantId, ManagedResource resource, long id);

    Map<String, Object> create(long tenantId, ManagedResource resource, Map<String, Object> values);

    Map<String, Object> update(long tenantId, ManagedResource resource, long id, long version,
                               Map<String, Object> values);

    boolean hasEnabledReference(long tenantId, ManagedResource resource, String column, long id);
}
