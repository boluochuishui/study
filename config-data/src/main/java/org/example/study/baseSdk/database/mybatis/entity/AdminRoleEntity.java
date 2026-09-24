package org.example.study.baseSdk.database.mybatis.entity;

import lombok.Getter;
import lombok.Setter;

/** RBAC 角色查询实体。 */
@Getter
@Setter
public class AdminRoleEntity {
    private Long id;
    private Long tenantId;
    private String roleCode;
    private String roleName;
    private Boolean enabled;
}
