package org.example.study.baseSdk.database.mybatis.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.example.study.baseSdk.database.mybatis.entity.AdminRoleEntity;
import org.example.study.baseSdk.database.mybatis.entity.AdminPermissionEntity;
import org.example.study.baseSdk.database.mybatis.entity.AdminUserEntity;

/** 管理台 RBAC Mapper，SQL 统一保存在 XML。 */
public interface AdminSecurityMapper {

    AdminUserEntity selectUserForLogin(@Param("tenantCode") String tenantCode,
                                       @Param("username") String username);

    AdminUserEntity selectIdentityByToken(@Param("tokenHash") String tokenHash,
                                          @Param("now") LocalDateTime now);

    List<String> selectUserPermissions(@Param("tenantId") long tenantId,
                                       @Param("userId") long userId);

    int insertAccessToken(@Param("tenantId") long tenantId, @Param("userId") long userId,
                          @Param("tokenHash") String tokenHash, @Param("expiresAt") LocalDateTime expiresAt);

    int revokeAccessToken(@Param("tokenHash") String tokenHash);

    long countUsers(@Param("tenantCode") String tenantCode);

    int insertUser(AdminUserEntity user);

    List<AdminUserEntity> selectUsers(@Param("tenantId") long tenantId);

    List<AdminRoleEntity> selectRoles(@Param("tenantId") long tenantId);

    AdminRoleEntity selectRoleByCode(@Param("tenantId") long tenantId,
                                     @Param("roleCode") String roleCode);

    List<AdminPermissionEntity> selectPermissions();

    int upsertRole(@Param("tenantId") long tenantId, @Param("roleCode") String roleCode,
                   @Param("roleName") String roleName, @Param("enabled") boolean enabled);

    int deleteRolePermissions(@Param("tenantId") long tenantId,
                              @Param("roleCode") String roleCode);

    int insertRolePermissionByCode(@Param("tenantId") long tenantId,
                                   @Param("roleCode") String roleCode,
                                   @Param("permissionCode") String permissionCode);

    List<String> selectRolePermissions(@Param("roleId") long roleId);

    int deleteUserRoles(@Param("tenantId") long tenantId, @Param("userId") long userId);

    int insertUserRoleByCode(@Param("tenantId") long tenantId, @Param("userId") long userId,
                             @Param("roleCode") String roleCode);

    Long selectTenantId(@Param("tenantCode") String tenantCode);

    long countUserInTenant(@Param("tenantId") long tenantId, @Param("userId") long userId);
}
