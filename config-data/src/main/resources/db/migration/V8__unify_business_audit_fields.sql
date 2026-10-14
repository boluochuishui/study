-- 创建人、更新人保存用户 ID；0 表示系统执行或历史操作人未知。
-- 不修改已有创建/更新时间。历史缺失的时间使用已有可追溯时间或迁移时间补齐。
ALTER TABLE admin_tenant
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统或未知',
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统或未知';

ALTER TABLE admin_user
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统或未知',
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统或未知';

ALTER TABLE admin_role
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统或未知',
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统或未知';

ALTER TABLE admin_permission
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统或未知',
    ADD COLUMN created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统或未知',
    ADD COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间';

ALTER TABLE admin_user_role
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统或未知',
    ADD COLUMN created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统或未知',
    ADD COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间';

ALTER TABLE admin_role_permission
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统或未知',
    ADD COLUMN created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统或未知',
    ADD COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间';

ALTER TABLE admin_access_token
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统或未知',
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统或未知',
    ADD COLUMN updated_at DATETIME(3) NULL COMMENT '更新时间';
UPDATE admin_access_token SET created_by = user_id, updated_at = created_at;
ALTER TABLE admin_access_token
    MODIFY COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间';

ALTER TABLE sdk_config_item
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统或未知',
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统或未知';

ALTER TABLE sdk_spel_rule_set
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统或未知',
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统或未知';

ALTER TABLE sdk_spel_rule_draft
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统或未知',
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统或未知';

ALTER TABLE sdk_spel_rule_release
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统或未知',
    ADD COLUMN created_at DATETIME(3) NULL COMMENT '创建时间',
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统或未知',
    ADD COLUMN updated_at DATETIME(3) NULL COMMENT '更新时间';
UPDATE sdk_spel_rule_release SET created_at = published_at, updated_at = published_at;
ALTER TABLE sdk_spel_rule_release
    MODIFY COLUMN created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    MODIFY COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间';

ALTER TABLE admin_online_test_record
    ADD COLUMN created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统或未知',
    ADD COLUMN updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统或未知',
    ADD COLUMN updated_at DATETIME(3) NULL COMMENT '更新时间';
UPDATE admin_online_test_record SET created_by = user_id, updated_by = user_id, updated_at = created_at;
ALTER TABLE admin_online_test_record
    MODIFY COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间';
