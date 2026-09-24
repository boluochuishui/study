package org.example.study.admin.service;

import java.util.List;

import org.example.study.admin.api.AdminApiException;
import org.example.study.admin.api.AdminUserView;
import org.example.study.admin.api.CreateAdminUserRequest;
import org.example.study.admin.api.ReplaceUserRolesRequest;
import org.example.study.admin.api.SaveAdminRoleRequest;
import org.example.study.admin.security.AdminPermissions;
import org.example.study.admin.security.AdminSecurityContext;
import org.example.study.baseSdk.database.api.model.AdminRoleData;
import org.example.study.baseSdk.database.api.model.AdminPermissionData;
import org.example.study.baseSdk.database.api.model.AdminUserData;
import org.example.study.baseSdk.database.api.service.AdminSecurityDataService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 当前租户内的用户和角色管理服务。 */
@Service
public class AdminIamService {

    private final AdminSecurityDataService dataService;
    private final PasswordEncoder passwordEncoder;

    public AdminIamService(AdminSecurityDataService dataService, PasswordEncoder passwordEncoder) {
        this.dataService = dataService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AdminUserView> listUsers() {
        long tenantId = AdminSecurityContext.requirePrincipal().tenantId();
        return dataService.listUsers(tenantId).stream().map(this::toView).toList();
    }

    public List<AdminRoleData> listRoles() {
        return dataService.listRoles(AdminSecurityContext.requirePrincipal().tenantId());
    }

    public List<AdminPermissionData> listPermissions() {
        return dataService.listPermissions();
    }

    public AdminRoleData saveRole(String roleCode, SaveAdminRoleRequest request) {
        if (invalidIdentifier(roleCode) || AdminPermissions.SUPER_ADMIN_ROLE.equals(roleCode)
                || request == null || request.roleName() == null || request.roleName().isBlank()
                || request.roleName().length() > 128 || request.permissionCodes() == null
                || request.permissionCodes().isEmpty()
                || request.permissionCodes().stream().anyMatch(this::invalidPermissionCode)) {
            throw invalidRequest();
        }
        try {
            return dataService.saveRole(AdminSecurityContext.requirePrincipal().tenantId(), roleCode,
                    request.roleName(), request.enabled(), request.permissionCodes());
        } catch (IllegalArgumentException exception) {
            throw new AdminApiException("IAM_NOT_FOUND", "Admin permission not found",
                    HttpStatus.NOT_FOUND, exception);
        }
    }

    @Transactional
    public AdminUserView createUser(CreateAdminUserRequest request) {
        validateCreateRequest(request);
        long tenantId = AdminSecurityContext.requirePrincipal().tenantId();
        try {
            AdminUserData user = dataService.createUser(tenantId, request.username(), request.displayName(),
                    passwordEncoder.encode(request.password()));
            dataService.replaceUserRoles(tenantId, user.id(), request.roleCodes());
            return toView(user);
        } catch (IllegalArgumentException exception) {
            throw new AdminApiException("IAM_CONFLICT", "Unable to create admin user",
                    HttpStatus.CONFLICT, exception);
        }
    }

    public void replaceRoles(long userId, ReplaceUserRolesRequest request) {
        if (userId <= 0 || request == null || request.roleCodes() == null || request.roleCodes().isEmpty()
                || request.roleCodes().stream().anyMatch(this::invalidIdentifier)) {
            throw invalidRequest();
        }
        try {
            dataService.replaceUserRoles(AdminSecurityContext.requirePrincipal().tenantId(),
                    userId, request.roleCodes());
        } catch (IllegalArgumentException exception) {
            throw new AdminApiException("IAM_NOT_FOUND", "Admin user or role not found",
                    HttpStatus.NOT_FOUND, exception);
        }
    }

    private void validateCreateRequest(CreateAdminUserRequest request) {
        if (request == null || invalidIdentifier(request.username())
                || request.displayName() == null || request.displayName().isBlank()
                || request.displayName().length() > 128 || request.password() == null
                || request.password().length() < 12 || request.password().length() > 128
                || request.roleCodes() == null || request.roleCodes().isEmpty()
                || request.roleCodes().stream().anyMatch(this::invalidIdentifier)) {
            throw invalidRequest();
        }
    }

    private boolean invalidIdentifier(String value) {
        return value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    }

    private boolean invalidPermissionCode(String value) {
        return value == null || !value.matches("[A-Z][A-Z0-9_]{0,63}");
    }

    private AdminApiException invalidRequest() {
        return new AdminApiException("INVALID_REQUEST", "Invalid IAM request", HttpStatus.BAD_REQUEST);
    }

    private AdminUserView toView(AdminUserData user) {
        return new AdminUserView(user.id(), user.tenantId(), user.username(), user.displayName(), user.enabled());
    }
}
