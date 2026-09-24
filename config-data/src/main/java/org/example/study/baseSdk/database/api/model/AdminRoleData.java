package org.example.study.baseSdk.database.api.model;

import java.util.Set;

/** 租户角色及其权限集合。 */
public record AdminRoleData(long id, long tenantId, String roleCode, String roleName,
                            boolean enabled, Set<String> permissions) {
    public AdminRoleData {
        permissions = Set.copyOf(permissions);
    }
}
