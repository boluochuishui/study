package org.example.study.baseSdk.database.mybatis.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.study.baseSdk.database.api.audit.DatabaseAuditContext;
import org.example.study.baseSdk.database.api.exception.ConfigConflictException;
import org.example.study.baseSdk.database.api.exception.DatabaseSdkException;
import org.example.study.baseSdk.database.api.model.ManagedResource;
import org.example.study.baseSdk.database.api.service.ManagedResourceDataService;
import org.example.study.baseSdk.database.mybatis.mapper.ManagedResourceMapper;
import org.example.study.baseSdk.database.mybatis.model.ManagedMutation;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 配置表通用数据访问，表名和列名只允许枚举值。 */
@Service
public class ManagedResourceDataServiceImpl implements ManagedResourceDataService {
    private final ManagedResourceMapper mapper;

    public ManagedResourceDataServiceImpl(ManagedResourceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(long tenantId, ManagedResource resource) {
        requireTenant(tenantId);
        try {
            return mapper.list(resource.table(), tenantId);
        } catch (DataAccessException exception) {
            throw new DatabaseSdkException("Failed to list managed resources", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> find(long tenantId, ManagedResource resource, long id) {
        requireTenant(tenantId);
        if (id < 1) {
            throw new IllegalArgumentException("Resource ID must be positive");
        }
        try {
            return mapper.find(resource.table(), tenantId, id);
        } catch (DataAccessException exception) {
            throw new DatabaseSdkException("Failed to query managed resource", exception);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> create(long tenantId, ManagedResource resource, Map<String, Object> values) {
        requireTenant(tenantId);
        ManagedMutation mutation = mutation(null, tenantId, resource, 0, values);
        try {
            if (mapper.insert(mutation) != 1 || mutation.getId() == null) {
                throw new DatabaseSdkException("Failed to create managed resource");
            }
            return mapper.find(resource.table(), tenantId, mutation.getId());
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Managed resource violates a database constraint", exception);
        } catch (DataAccessException exception) {
            throw new DatabaseSdkException("Failed to create managed resource", exception);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> update(long tenantId, ManagedResource resource, long id, long version,
                                      Map<String, Object> values) {
        requireTenant(tenantId);
        if (id < 1 || version < 1) {
            throw new IllegalArgumentException("Resource ID and version must be positive");
        }
        ManagedMutation mutation = mutation(id, tenantId, resource, version, values);
        try {
            if (mapper.update(mutation) != 1) {
                throw new ConfigConflictException();
            }
            return mapper.find(resource.table(), tenantId, id);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Managed resource violates a database constraint", exception);
        } catch (DataAccessException exception) {
            throw new DatabaseSdkException("Failed to update managed resource", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasEnabledReference(long tenantId, ManagedResource resource, String column, long id) {
        requireTenant(tenantId);
        if (!resource.columns().contains(column) || id < 1) {
            throw new IllegalArgumentException("Invalid managed resource reference");
        }
        try {
            return mapper.hasEnabledReference(resource.table(), tenantId, column, id) != null;
        } catch (DataAccessException exception) {
            throw new DatabaseSdkException("Failed to query managed resource reference", exception);
        }
    }

    private ManagedMutation mutation(Long id, long tenantId, ManagedResource resource, long version,
                                     Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Resource values must not be empty");
        }
        Map<String, Object> ordered = new LinkedHashMap<>(values);
        if (!resource.columns().containsAll(ordered.keySet())) {
            throw new IllegalArgumentException("Unknown managed resource field");
        }
        List<ManagedMutation.FieldValue> fields = new ArrayList<>();
        for (String column : resource.columns()) {
            if (ordered.containsKey(column)) {
                fields.add(new ManagedMutation.FieldValue(column, ordered.get(column)));
            }
        }
        return new ManagedMutation(id, resource.table(), tenantId, DatabaseAuditContext.actorId(), version, fields);
    }

    private void requireTenant(long tenantId) {
        if (tenantId < 1) {
            throw new IllegalArgumentException("Tenant ID must be positive");
        }
    }
}
