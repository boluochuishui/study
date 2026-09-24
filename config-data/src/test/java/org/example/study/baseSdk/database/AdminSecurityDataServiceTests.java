package org.example.study.baseSdk.database;

import java.util.List;

import org.example.study.baseSdk.database.mybatis.entity.AdminRoleEntity;
import org.example.study.baseSdk.database.mybatis.mapper.AdminSecurityMapper;
import org.example.study.baseSdk.database.mybatis.service.AdminSecurityDataServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证 RBAC 数据服务始终携带租户边界。 */
class AdminSecurityDataServiceTests {

    private final AdminSecurityMapper mapper = mock(AdminSecurityMapper.class);
    private final AdminSecurityDataServiceImpl service = new AdminSecurityDataServiceImpl(mapper);

    @Test
    void savesRolePermissionSetWithinTenant() {
        AdminRoleEntity role = new AdminRoleEntity();
        role.setId(9L);
        role.setTenantId(7L);
        role.setRoleCode("AUDITOR");
        role.setRoleName("Auditor");
        role.setEnabled(true);
        when(mapper.insertRolePermissionByCode(7, "AUDITOR", "CONFIG_READ")).thenReturn(1);
        when(mapper.insertRolePermissionByCode(7, "AUDITOR", "RULE_READ")).thenReturn(1);
        when(mapper.selectRoleByCode(7, "AUDITOR")).thenReturn(role);
        when(mapper.selectRolePermissions(9)).thenReturn(List.of("CONFIG_READ", "RULE_READ"));

        var saved = service.saveRole(7, "AUDITOR", "Auditor", true,
                List.of("CONFIG_READ", "RULE_READ", "CONFIG_READ"));

        assertThat(saved.tenantId()).isEqualTo(7);
        assertThat(saved.permissions()).containsExactlyInAnyOrder("CONFIG_READ", "RULE_READ");
        verify(mapper).upsertRole(7, "AUDITOR", "Auditor", true);
        verify(mapper).deleteRolePermissions(7, "AUDITOR");
    }
}
