package org.example.study.admin.api;

import java.util.List;

/** 覆盖当前租户内用户的角色集合。 */
public record ReplaceUserRolesRequest(List<String> roleCodes) {
    public ReplaceUserRolesRequest {
        roleCodes = roleCodes == null ? null : List.copyOf(roleCodes);
    }
}
