package org.example.study.baseSdk.database.mybatis.entity;

import lombok.Getter;
import lombok.Setter;

/** RBAC 用户及身份联合查询实体。 */
@Getter
@Setter
public class AdminUserEntity {
    private Long id;
    private Long tenantId;
    private String tenantCode;
    private String username;
    private String displayName;
    private String passwordHash;
    private Boolean enabled;
}
