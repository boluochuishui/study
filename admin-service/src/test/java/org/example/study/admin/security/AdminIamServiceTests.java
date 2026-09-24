package org.example.study.admin.security;

import java.util.List;
import java.util.Set;

import org.example.study.admin.api.AdminApiException;
import org.example.study.admin.api.CreateAdminUserRequest;
import org.example.study.admin.api.ReplaceUserRolesRequest;
import org.example.study.admin.api.SaveAdminRoleRequest;
import org.example.study.admin.service.AdminIamService;
import org.example.study.baseSdk.database.api.model.AdminPermissionData;
import org.example.study.baseSdk.database.api.model.AdminRoleData;
import org.example.study.baseSdk.database.api.model.AdminUserData;
import org.example.study.baseSdk.database.api.service.AdminSecurityDataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 当前租户 IAM 业务行为测试。 */
class AdminIamServiceTests {

    private final AdminSecurityDataService dataService = mock(AdminSecurityDataService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AdminIamService service = new AdminIamService(dataService, passwordEncoder);

    @BeforeEach
    void setPrincipal() {
        AdminSecurityContext.set(new AdminPrincipal(3, 7, "tenant-a", "admin", "Admin",
                "token-hash", Set.of(AdminPermissions.IAM_READ, AdminPermissions.IAM_WRITE)));
    }

    @AfterEach
    void clearPrincipal() {
        AdminSecurityContext.clear();
    }

    @Test
    void listsOnlyCurrentTenantIamData() {
        var user = new AdminUserData(11L, 7, "tenant-a", "alice", "Alice", "hash", true);
        var role = new AdminRoleData(9, 7, "AUDITOR", "Auditor", true, Set.of("CONFIG_READ"));
        var permission = new AdminPermissionData("CONFIG_READ", "Read configurations");
        when(dataService.listUsers(7)).thenReturn(List.of(user));
        when(dataService.listRoles(7)).thenReturn(List.of(role));
        when(dataService.listPermissions()).thenReturn(List.of(permission));

        assertThat(service.listUsers()).extracting("username").containsExactly("alice");
        assertThat(service.listRoles()).containsExactly(role);
        assertThat(service.listPermissions()).containsExactly(permission);
        verify(dataService).listUsers(7);
        verify(dataService).listRoles(7);
    }

    @Test
    void createsUserWithHashedPasswordAndTenantRoles() {
        when(passwordEncoder.encode("StrongPassword123")).thenReturn("bcrypt-hash");
        var user = new AdminUserData(11L, 7, null, "alice", "Alice", "bcrypt-hash", true);
        when(dataService.createUser(7, "alice", "Alice", "bcrypt-hash")).thenReturn(user);

        var created = service.createUser(new CreateAdminUserRequest(
                "alice", "Alice", "StrongPassword123", List.of("AUDITOR")));

        assertThat(created.username()).isEqualTo("alice");
        verify(dataService).replaceUserRoles(7, 11, List.of("AUDITOR"));
    }

    @Test
    void replacesUserRolesWithinCurrentTenant() {
        service.replaceRoles(11, new ReplaceUserRolesRequest(List.of("AUDITOR", "RULE_EDITOR")));

        verify(dataService).replaceUserRoles(7, 11, List.of("AUDITOR", "RULE_EDITOR"));
    }

    @Test
    void savesCustomRolePermissionSetWithinCurrentTenant() {
        var request = new SaveAdminRoleRequest("Auditor", true, List.of("CONFIG_READ", "RULE_READ"));
        var role = new AdminRoleData(9, 7, "AUDITOR", "Auditor", true,
                Set.of("CONFIG_READ", "RULE_READ"));
        when(dataService.saveRole(7, "AUDITOR", "Auditor", true,
                List.of("CONFIG_READ", "RULE_READ"))).thenReturn(role);

        assertThat(service.saveRole("AUDITOR", request)).isEqualTo(role);
    }

    @Test
    void protectsBuiltInSuperAdminRole() {
        var request = new SaveAdminRoleRequest("Changed", true, List.of("IAM_READ"));

        assertThatThrownBy(() -> service.saveRole(AdminPermissions.SUPER_ADMIN_ROLE, request))
                .isInstanceOf(AdminApiException.class)
                .hasMessage("Invalid IAM request");
    }
}
