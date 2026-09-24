CREATE TABLE IF NOT EXISTS admin_tenant
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '租户主键',
    tenant_code VARCHAR(64)     NOT NULL COMMENT '租户编码',
    tenant_name VARCHAR(128)    NOT NULL COMMENT '租户名称',
    enabled     TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_tenant_code (tenant_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理台租户';

INSERT INTO admin_tenant (id, tenant_code, tenant_name, enabled)
VALUES (1, 'default', 'Default Tenant', 1)
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name);

CREATE TABLE IF NOT EXISTS admin_user
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    tenant_id     BIGINT UNSIGNED NOT NULL COMMENT '租户主键',
    username      VARCHAR(64)     NOT NULL COMMENT '用户名',
    display_name  VARCHAR(128)    NOT NULL COMMENT '显示名称',
    password_hash VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密码摘要',
    enabled       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_user_tenant_name (tenant_id, username),
    CONSTRAINT fk_admin_user_tenant FOREIGN KEY (tenant_id) REFERENCES admin_tenant (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理台用户';

CREATE TABLE IF NOT EXISTS admin_role
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色主键',
    tenant_id   BIGINT UNSIGNED NOT NULL COMMENT '租户主键',
    role_code   VARCHAR(64)     NOT NULL COMMENT '角色编码',
    role_name   VARCHAR(128)    NOT NULL COMMENT '角色名称',
    enabled     TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_role_tenant_code (tenant_id, role_code),
    CONSTRAINT fk_admin_role_tenant FOREIGN KEY (tenant_id) REFERENCES admin_tenant (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理台角色';

CREATE TABLE IF NOT EXISTS admin_permission
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限主键',
    permission_code VARCHAR(64)     NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(128)    NOT NULL COMMENT '权限名称',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_permission_code (permission_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理台权限目录';

CREATE TABLE IF NOT EXISTS admin_user_role
(
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '租户主键',
    user_id   BIGINT UNSIGNED NOT NULL COMMENT '用户主键',
    role_id   BIGINT UNSIGNED NOT NULL COMMENT '角色主键',
    PRIMARY KEY (tenant_id, user_id, role_id),
    CONSTRAINT fk_admin_user_role_tenant FOREIGN KEY (tenant_id) REFERENCES admin_tenant (id),
    CONSTRAINT fk_admin_user_role_user FOREIGN KEY (user_id) REFERENCES admin_user (id),
    CONSTRAINT fk_admin_user_role_role FOREIGN KEY (role_id) REFERENCES admin_role (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理台用户角色关联';

CREATE TABLE IF NOT EXISTS admin_role_permission
(
    role_id       BIGINT UNSIGNED NOT NULL COMMENT '角色主键',
    permission_id BIGINT UNSIGNED NOT NULL COMMENT '权限主键',
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_admin_role_permission_role FOREIGN KEY (role_id) REFERENCES admin_role (id),
    CONSTRAINT fk_admin_role_permission_permission FOREIGN KEY (permission_id) REFERENCES admin_permission (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理台角色权限关联';

CREATE TABLE IF NOT EXISTS admin_access_token
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '令牌主键',
    tenant_id  BIGINT UNSIGNED NOT NULL COMMENT '租户主键',
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户主键',
    token_hash CHAR(64)        NOT NULL COMMENT '令牌 SHA-256 摘要',
    expires_at DATETIME(3)     NOT NULL COMMENT '过期时间',
    revoked    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否撤销',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_access_token_hash (token_hash),
    KEY idx_admin_access_token_user (tenant_id, user_id, revoked),
    CONSTRAINT fk_admin_access_token_tenant FOREIGN KEY (tenant_id) REFERENCES admin_tenant (id),
    CONSTRAINT fk_admin_access_token_user FOREIGN KEY (user_id) REFERENCES admin_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理台访问令牌';

INSERT INTO admin_permission (permission_code, permission_name)
VALUES ('CONFIG_READ', 'Read configurations'),
       ('CONFIG_WRITE', 'Write configurations'),
       ('RULE_READ', 'Read rules'),
       ('RULE_WRITE', 'Write rule drafts'),
       ('RULE_PUBLISH', 'Publish and rollback rules'),
       ('IAM_READ', 'Read users and roles'),
       ('IAM_WRITE', 'Manage users and roles')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT INTO admin_role (tenant_id, role_code, role_name, enabled)
VALUES (1, 'SUPER_ADMIN', 'Super Administrator', 1)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), enabled = 1;

INSERT IGNORE INTO admin_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM admin_role r
JOIN admin_permission p
WHERE r.tenant_id = 1 AND r.role_code = 'SUPER_ADMIN';

ALTER TABLE sdk_config_item ADD COLUMN tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '租户主键' AFTER id;
ALTER TABLE sdk_config_item DROP INDEX uk_namespace_config_key;
ALTER TABLE sdk_config_item ADD UNIQUE KEY uk_config_tenant_namespace_key (tenant_id, namespace, config_key);
ALTER TABLE sdk_config_item ADD KEY idx_config_tenant_namespace_enabled (tenant_id, namespace, enabled);

ALTER TABLE sdk_spel_rule_set ADD COLUMN tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '租户主键' AFTER id;
ALTER TABLE sdk_spel_rule_set DROP INDEX uk_spel_rule_set_code;
ALTER TABLE sdk_spel_rule_set ADD UNIQUE KEY uk_spel_rule_set_tenant_code (tenant_id, rule_set_code);
ALTER TABLE sdk_spel_rule_set ADD KEY idx_spel_rule_set_tenant_scene (tenant_id, scene, enabled);

ALTER TABLE sdk_spel_rule_draft ADD COLUMN tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '租户主键' AFTER id;
ALTER TABLE sdk_spel_rule_draft DROP INDEX uk_spel_rule_draft;
ALTER TABLE sdk_spel_rule_draft ADD UNIQUE KEY uk_spel_rule_draft_tenant (tenant_id, rule_set_id, rule_id);
ALTER TABLE sdk_spel_rule_draft ADD KEY idx_spel_rule_draft_tenant_order (tenant_id, rule_set_id, priority, rule_id);

ALTER TABLE sdk_spel_rule_release ADD COLUMN tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '租户主键' AFTER release_id;
ALTER TABLE sdk_spel_rule_release DROP INDEX uk_spel_rule_release_version;
ALTER TABLE sdk_spel_rule_release ADD UNIQUE KEY uk_spel_rule_release_tenant_version
    (tenant_id, rule_set_code, release_version);
ALTER TABLE sdk_spel_rule_release ADD KEY idx_spel_rule_release_tenant_scene_cursor
    (tenant_id, scene, release_id);
