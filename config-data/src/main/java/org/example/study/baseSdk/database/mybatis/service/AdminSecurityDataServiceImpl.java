package org.example.study.baseSdk.database.mybatis.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.example.study.baseSdk.database.api.exception.DatabaseSdkException;
import org.example.study.baseSdk.database.api.model.AdminIdentityData;
import org.example.study.baseSdk.database.api.model.AdminPermissionData;
import org.example.study.baseSdk.database.api.model.AdminRoleData;
import org.example.study.baseSdk.database.api.model.AdminUserData;
import org.example.study.baseSdk.database.api.service.AdminSecurityDataService;
import org.example.study.baseSdk.database.mybatis.entity.AdminRoleEntity;
import org.example.study.baseSdk.database.mybatis.entity.AdminUserEntity;
import org.example.study.baseSdk.database.mybatis.mapper.AdminSecurityMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理台认证与 RBAC 数据服务实现。 */
@Service
public class AdminSecurityDataServiceImpl implements AdminSecurityDataService {

    private final AdminSecurityMapper mapper;

    public AdminSecurityDataServiceImpl(AdminSecurityMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminUserData> findUserForLogin(String tenantCode, String username) {
        requireText(tenantCode, "Tenant code must not be blank");
        requireText(username, "Username must not be blank");
        return Optional.ofNullable(mapper.selectUserForLogin(tenantCode, username)).map(this::toUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminIdentityData> findIdentityByTokenHash(String tokenHash, LocalDateTime now) {
        requireText(tokenHash, "Token hash must not be blank");
        if (now == null) {
            throw new IllegalArgumentException("Token query time must not be null");
        }
        AdminUserEntity user = mapper.selectIdentityByToken(tokenHash, now);
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(new AdminIdentityData(user.getId(), user.getTenantId(), user.getTenantCode(),
                user.getUsername(), user.getDisplayName(),
                new LinkedHashSet<>(mapper.selectUserPermissions(user.getTenantId(), user.getId()))));
    }

    @Override
    @Transactional
    public void saveAccessToken(long tenantId, long userId, String tokenHash, LocalDateTime expiresAt) {
        validateIds(tenantId, userId);
        requireText(tokenHash, "Token hash must not be blank");
        if (expiresAt == null || mapper.insertAccessToken(tenantId, userId, tokenHash, expiresAt) != 1) {
            throw new DatabaseSdkException("Failed to save admin access token");
        }
    }

    @Override
    @Transactional
    public boolean revokeAccessToken(String tokenHash) {
        requireText(tokenHash, "Token hash must not be blank");
        return mapper.revokeAccessToken(tokenHash) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsers(String tenantCode) {
        requireText(tenantCode, "Tenant code must not be blank");
        return mapper.countUsers(tenantCode);
    }

    @Override
    @Transactional
    public AdminUserData createUser(long tenantId, String username, String displayName, String passwordHash) {
        validateIds(tenantId, 1);
        requireText(username, "Username must not be blank");
        requireText(displayName, "Display name must not be blank");
        requireText(passwordHash, "Password hash must not be blank");
        AdminUserEntity entity = new AdminUserEntity();
        entity.setTenantId(tenantId);
        entity.setUsername(username);
        entity.setDisplayName(displayName);
        entity.setPasswordHash(passwordHash);
        entity.setEnabled(true);
        try {
            if (mapper.insertUser(entity) != 1) {
                throw new DatabaseSdkException("Failed to create admin user");
            }
            return toUser(entity);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("Admin user already exists", exception);
        } catch (DataAccessException exception) {
            throw new DatabaseSdkException("Failed to create admin user", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserData> listUsers(long tenantId) {
        validateIds(tenantId, 1);
        return mapper.selectUsers(tenantId).stream().map(this::toUser).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminRoleData> listRoles(long tenantId) {
        validateIds(tenantId, 1);
        return mapper.selectRoles(tenantId).stream().map(this::toRole).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminPermissionData> listPermissions() {
        return mapper.selectPermissions().stream()
                .map(value -> new AdminPermissionData(value.getPermissionCode(), value.getPermissionName()))
                .toList();
    }

    @Override
    @Transactional
    public AdminRoleData saveRole(long tenantId, String roleCode, String roleName,
                                  boolean enabled, List<String> permissionCodes) {
        validateIds(tenantId, 1);
        requireText(roleCode, "Role code must not be blank");
        requireText(roleName, "Role name must not be blank");
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            throw new IllegalArgumentException("Role permissions must not be empty");
        }
        permissionCodes.forEach(code -> requireText(code, "Permission code must not be blank"));
        mapper.upsertRole(tenantId, roleCode, roleName, enabled);
        mapper.deleteRolePermissions(tenantId, roleCode);
        for (String permissionCode : permissionCodes.stream().distinct().toList()) {
            if (mapper.insertRolePermissionByCode(tenantId, roleCode, permissionCode) != 1) {
                throw new IllegalArgumentException("Admin permission does not exist: " + permissionCode);
            }
        }
        AdminRoleEntity role = mapper.selectRoleByCode(tenantId, roleCode);
        if (role == null) {
            throw new DatabaseSdkException("Failed to resolve saved admin role");
        }
        return toRole(role);
    }

    @Override
    @Transactional
    public void replaceUserRoles(long tenantId, long userId, List<String> roleCodes) {
        validateUserAndRoles(tenantId, userId, roleCodes);
        mapper.deleteUserRoles(tenantId, userId);
        for (String roleCode : roleCodes.stream().distinct().toList()) {
            if (mapper.insertUserRoleByCode(tenantId, userId, roleCode) != 1) {
                throw new IllegalArgumentException("Admin role does not exist: " + roleCode);
            }
        }
    }

    @Override
    @Transactional
    public void assignRole(long tenantId, long userId, String roleCode) {
        validateUserAndRoles(tenantId, userId, List.of(roleCode));
        if (mapper.insertUserRoleByCode(tenantId, userId, roleCode) != 1) {
            throw new IllegalArgumentException("Admin role does not exist: " + roleCode);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long requireTenantId(String tenantCode) {
        requireText(tenantCode, "Tenant code must not be blank");
        Long tenantId = mapper.selectTenantId(tenantCode);
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant does not exist: " + tenantCode);
        }
        return tenantId;
    }

    private void validateUserAndRoles(long tenantId, long userId, List<String> roleCodes) {
        validateIds(tenantId, userId);
        if (roleCodes == null || mapper.countUserInTenant(tenantId, userId) != 1) {
            throw new IllegalArgumentException("Admin user does not exist in tenant");
        }
        roleCodes.forEach(code -> requireText(code, "Role code must not be blank"));
    }

    private AdminUserData toUser(AdminUserEntity entity) {
        return new AdminUserData(entity.getId(), entity.getTenantId(), entity.getTenantCode(), entity.getUsername(),
                entity.getDisplayName(), entity.getPasswordHash(), Boolean.TRUE.equals(entity.getEnabled()));
    }

    private AdminRoleData toRole(AdminRoleEntity entity) {
        return new AdminRoleData(entity.getId(), entity.getTenantId(), entity.getRoleCode(), entity.getRoleName(),
                Boolean.TRUE.equals(entity.getEnabled()),
                new LinkedHashSet<>(mapper.selectRolePermissions(entity.getId())));
    }

    private void validateIds(long tenantId, long userId) {
        if (tenantId <= 0 || userId <= 0) {
            throw new IllegalArgumentException("Tenant and user ids must be positive");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
