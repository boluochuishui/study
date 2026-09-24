package org.example.study.admin.api;

import java.util.List;

/** 在当前租户创建管理用户。 */
public record CreateAdminUserRequest(String username, String displayName, String password,
                                     List<String> roleCodes) {
    public CreateAdminUserRequest {
        roleCodes = roleCodes == null ? null : List.copyOf(roleCodes);
    }
}
