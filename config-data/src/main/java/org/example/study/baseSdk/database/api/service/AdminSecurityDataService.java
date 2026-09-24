package org.example.study.baseSdk.database.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.example.study.baseSdk.database.api.model.AdminIdentityData;
import org.example.study.baseSdk.database.api.model.AdminPermissionData;
import org.example.study.baseSdk.database.api.model.AdminRoleData;
import org.example.study.baseSdk.database.api.model.AdminUserData;

/** 管理台认证、令牌和 RBAC 数据访问边界。 */
public interface AdminSecurityDataService {

    Optional<AdminUserData> findUserForLogin(String tenantCode, String username);

    Optional<AdminIdentityData> findIdentityByTokenHash(String tokenHash, LocalDateTime now);

    void saveAccessToken(long tenantId, long userId, String tokenHash, LocalDateTime expiresAt);

    boolean revokeAccessToken(String tokenHash);

    long countUsers(String tenantCode);

    AdminUserData createUser(long tenantId, String username, String displayName, String passwordHash);

    List<AdminUserData> listUsers(long tenantId);

    List<AdminRoleData> listRoles(long tenantId);

    List<AdminPermissionData> listPermissions();

    AdminRoleData saveRole(long tenantId, String roleCode, String roleName,
                           boolean enabled, List<String> permissionCodes);

    void replaceUserRoles(long tenantId, long userId, List<String> roleCodes);

    void assignRole(long tenantId, long userId, String roleCode);

    long requireTenantId(String tenantCode);
}
