package org.example.study.admin.api;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.example.study.admin.security.AdminPermissions;
import org.example.study.admin.security.RequireAdminPermission;
import org.example.study.admin.service.AdminIamService;
import org.example.study.baseSdk.database.api.model.AdminRoleData;
import org.example.study.baseSdk.database.api.model.AdminPermissionData;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前租户的用户和角色管理接口。 */
@RestController
@RequestMapping("/api/admin/iam")
public class AdminIamController {

    private final AdminIamService service;
    private final MessageSource messages;

    public AdminIamController(AdminIamService service, MessageSource messages) {
        this.service = service;
        this.messages = messages;
    }

    @GetMapping("/users")
    @RequireAdminPermission(AdminPermissions.IAM_READ)
    public AdminResponse<List<AdminUserView>> users(HttpServletRequest request) {
        return success(service.listUsers(), request);
    }

    @PostMapping("/users")
    @RequireAdminPermission(AdminPermissions.IAM_WRITE)
    public AdminResponse<AdminUserView> createUser(@RequestBody CreateAdminUserRequest body,
                                                   HttpServletRequest request) {
        return success(service.createUser(body), request);
    }

    @PutMapping("/users/{userId}/roles")
    @RequireAdminPermission(AdminPermissions.IAM_WRITE)
    public AdminResponse<Void> replaceRoles(@PathVariable long userId,
                                            @RequestBody ReplaceUserRolesRequest body,
                                            HttpServletRequest request) {
        service.replaceRoles(userId, body);
        return success(null, request);
    }

    @GetMapping("/roles")
    @RequireAdminPermission(AdminPermissions.IAM_READ)
    public AdminResponse<List<AdminRoleData>> roles(HttpServletRequest request) {
        return success(service.listRoles(), request);
    }

    @GetMapping("/permissions")
    @RequireAdminPermission(AdminPermissions.IAM_READ)
    public AdminResponse<List<AdminPermissionData>> permissions(HttpServletRequest request) {
        return success(service.listPermissions(), request);
    }

    @PutMapping("/roles/{roleCode}")
    @RequireAdminPermission(AdminPermissions.IAM_WRITE)
    public AdminResponse<AdminRoleData> saveRole(@PathVariable String roleCode,
                                                 @RequestBody SaveAdminRoleRequest body,
                                                 HttpServletRequest request) {
        return success(service.saveRole(roleCode, body), request);
    }

    private <T> AdminResponse<T> success(T data, HttpServletRequest request) {
        return new AdminResponse<>("SUCCESS", messages.getMessage("admin.success", null,
                AdminLocale.resolve(request)), data, Instant.now());
    }
}
