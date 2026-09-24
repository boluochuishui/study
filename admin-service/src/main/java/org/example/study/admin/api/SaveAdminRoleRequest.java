package org.example.study.admin.api;

import java.util.List;

/** 创建或更新租户角色的请求。 */
public record SaveAdminRoleRequest(String roleName, boolean enabled, List<String> permissionCodes) {
    public SaveAdminRoleRequest {
        permissionCodes = permissionCodes == null ? null : List.copyOf(permissionCodes);
    }
}
