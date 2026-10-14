-- 需求 033：媒资、App、场景和检测规则。需要先完成 V1 到 V8。
-- 所有引用均包含 tenant_id；不使用级联删除，不保存媒资正文或存储元数据。
ALTER TABLE sdk_spel_rule_release
    ADD UNIQUE KEY uk_spel_release_tenant_id (tenant_id, release_id);

CREATE TABLE media_library
(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '媒资库主键',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户',
    library_code VARCHAR(128) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '租户内唯一库编码',
    library_name VARCHAR(128) NOT NULL COMMENT '媒资库名称',
    content_type VARCHAR(16) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT 'TEXT/IMAGE/AUDIO/VIDEO',
    library_type VARCHAR(16) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT 'BLOCKLIST/ALLOWLIST/TESTSET',
    description VARCHAR(512) NULL COMMENT '用途说明',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否软删除',
    version BIGINT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_media_library_code (tenant_id, library_code),
    UNIQUE KEY uk_media_library_tenant_id (tenant_id, id),
    KEY idx_media_library_list (tenant_id, content_type, library_type, enabled, deleted),
    CONSTRAINT fk_media_library_tenant FOREIGN KEY (tenant_id) REFERENCES admin_tenant(id),
    CONSTRAINT ck_media_library_content_type CHECK (content_type IN ('TEXT', 'IMAGE', 'AUDIO', 'VIDEO')),
    CONSTRAINT ck_media_library_type CHECK (library_type IN ('BLOCKLIST', 'ALLOWLIST', 'TESTSET')),
    CONSTRAINT ck_media_library_state CHECK (enabled IN (0, 1) AND deleted IN (0, 1) AND version > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='媒资库';

CREATE TABLE media_asset
(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '媒资主键',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户',
    asset_name VARCHAR(255) NOT NULL COMMENT '媒资展示名称',
    content_type VARCHAR(16) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT 'TEXT/IMAGE/AUDIO/VIDEO',
    content_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '内容SHA-256十六进制摘要',
    hash_version VARCHAR(32) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '哈希前处理版本',
    storage_status VARCHAR(16) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT 'READY' COMMENT '样本准备状态：PENDING/READY/FAILED',
    description VARCHAR(512) NULL COMMENT '备注',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否软删除',
    version BIGINT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_media_asset_hash (tenant_id, content_type, hash_version, content_sha256),
    UNIQUE KEY uk_media_asset_tenant_id (tenant_id, id),
    KEY idx_media_asset_list (tenant_id, content_type, created_at, id),
    CONSTRAINT fk_media_asset_tenant FOREIGN KEY (tenant_id) REFERENCES admin_tenant(id),
    CONSTRAINT ck_media_asset_content_type CHECK (content_type IN ('TEXT', 'IMAGE', 'AUDIO', 'VIDEO')),
    CONSTRAINT ck_media_asset_hash CHECK (content_sha256 REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT ck_media_asset_status CHECK (storage_status IN ('PENDING', 'READY', 'FAILED')),
    CONSTRAINT ck_media_asset_state CHECK (enabled IN (0, 1) AND deleted IN (0, 1) AND version > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='媒资内容摘要及标注元信息';

CREATE TABLE media_asset_library
(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '媒资与库关联主键',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户',
    library_id BIGINT UNSIGNED NOT NULL COMMENT '媒资库内部ID',
    asset_id BIGINT UNSIGNED NOT NULL COMMENT '媒资内部ID',
    labels_json JSON NULL COMMENT '库内标注标签数组',
    description VARCHAR(512) NULL COMMENT '库内标注说明',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否软删除',
    version BIGINT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_media_asset_library_pair (tenant_id, library_id, asset_id),
    KEY idx_media_asset_library_reverse (tenant_id, asset_id, deleted),
    CONSTRAINT fk_media_asset_library_tenant FOREIGN KEY (tenant_id) REFERENCES admin_tenant(id),
    CONSTRAINT fk_media_asset_library_library FOREIGN KEY (tenant_id, library_id) REFERENCES media_library(tenant_id, id),
    CONSTRAINT fk_media_asset_library_asset FOREIGN KEY (tenant_id, asset_id) REFERENCES media_asset(tenant_id, id),
    CONSTRAINT ck_media_asset_library_labels CHECK (labels_json IS NULL OR JSON_TYPE(labels_json) = 'ARRAY'),
    CONSTRAINT ck_media_asset_library_state CHECK (enabled IN (0, 1) AND deleted IN (0, 1) AND version > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='媒资与媒资库多对多关联';

CREATE TABLE detect_app
(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'App内部主键',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户',
    app_id VARCHAR(128) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '外部App ID，对应X-App-Id',
    app_name VARCHAR(128) NOT NULL COMMENT '应用名称',
    credential_ref VARCHAR(255) NOT NULL COMMENT '服务端凭证引用，不保存明文密钥',
    description VARCHAR(512) NULL COMMENT '应用用途',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否软删除',
    version BIGINT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_detect_app_app_id (app_id),
    UNIQUE KEY uk_detect_app_tenant_id (tenant_id, id),
    KEY idx_detect_app_list (tenant_id, enabled, deleted),
    CONSTRAINT fk_detect_app_tenant FOREIGN KEY (tenant_id) REFERENCES admin_tenant(id),
    CONSTRAINT ck_detect_app_state CHECK (enabled IN (0, 1) AND deleted IN (0, 1) AND version > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检测接入应用';

CREATE TABLE detect_scene
(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '业务场景主键',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户',
    app_record_id BIGINT UNSIGNED NOT NULL COMMENT 'App内部主键',
    scene_code VARCHAR(128) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '当前App内场景编码，对应sceneCode',
    scene_name VARCHAR(128) NOT NULL COMMENT '场景名称',
    default_language VARCHAR(32) NULL COMMENT '默认语种',
    default_region VARCHAR(16) NULL COMMENT '默认地区',
    description VARCHAR(512) NULL COMMENT '场景用途',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否软删除',
    version BIGINT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_detect_scene_code (tenant_id, app_record_id, scene_code),
    UNIQUE KEY uk_detect_scene_tenant_id (tenant_id, id),
    KEY idx_detect_scene_list (tenant_id, app_record_id, enabled, deleted),
    CONSTRAINT fk_detect_scene_tenant FOREIGN KEY (tenant_id) REFERENCES admin_tenant(id),
    CONSTRAINT fk_detect_scene_app FOREIGN KEY (tenant_id, app_record_id) REFERENCES detect_app(tenant_id, id),
    CONSTRAINT ck_detect_scene_state CHECK (enabled IN (0, 1) AND deleted IN (0, 1) AND version > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='App业务场景';

CREATE TABLE detect_rule
(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '检测规则主键',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户',
    scene_id BIGINT UNSIGNED NOT NULL COMMENT '所属业务场景ID',
    rule_code VARCHAR(128) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT '场景内唯一规则编码',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    content_type VARCHAR(16) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT 'TEXT/IMAGE/AUDIO/VIDEO',
    rule_type VARCHAR(16) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT 'KEYWORD/REGEX/MEDIA_HASH/SPEL/LLM',
    execution_mode VARCHAR(8) COLLATE utf8mb4_0900_as_cs NOT NULL DEFAULT 'BOTH' COMMENT 'BOTH/SYNC/ASYNC',
    priority INT NOT NULL DEFAULT 100 COMMENT '节点内规则优先级，越小越先执行',
    on_hit_action VARCHAR(8) COLLATE utf8mb4_0900_as_cs NULL COMMENT '非SpEL命中动作：PASS/REVIEW/REJECT',
    risk_labels_json JSON NULL COMMENT '非SpEL风险标签数组',
    parameters_json JSON NULL COMMENT '引擎参数对象，不保存密钥或表达式编译对象',
    media_library_id BIGINT UNSIGNED NULL COMMENT '样本哈希规则引用的媒资库ID',
    spel_release_id BIGINT UNSIGNED NULL COMMENT 'SpEL已发布快照ID',
    spel_rule_id VARCHAR(128) COLLATE utf8mb4_0900_as_cs NULL COMMENT 'SpEL快照内规则标识',
    description VARCHAR(512) NULL COMMENT '规则说明',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否软删除',
    version BIGINT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人用户ID，0表示系统',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人用户ID，0表示系统',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_detect_rule_code (tenant_id, scene_id, rule_code),
    KEY idx_detect_rule_execute (tenant_id, scene_id, content_type, enabled, deleted, priority, id),
    KEY idx_detect_rule_library (tenant_id, media_library_id),
    KEY idx_detect_rule_spel_release (tenant_id, spel_release_id),
    CONSTRAINT fk_detect_rule_tenant FOREIGN KEY (tenant_id) REFERENCES admin_tenant(id),
    CONSTRAINT fk_detect_rule_scene FOREIGN KEY (tenant_id, scene_id) REFERENCES detect_scene(tenant_id, id),
    CONSTRAINT fk_detect_rule_library FOREIGN KEY (tenant_id, media_library_id) REFERENCES media_library(tenant_id, id),
    CONSTRAINT fk_detect_rule_spel_release FOREIGN KEY (tenant_id, spel_release_id) REFERENCES sdk_spel_rule_release(tenant_id, release_id),
    CONSTRAINT ck_detect_rule_content_type CHECK (content_type IN ('TEXT', 'IMAGE', 'AUDIO', 'VIDEO')),
    CONSTRAINT ck_detect_rule_type CHECK (rule_type IN ('KEYWORD', 'REGEX', 'MEDIA_HASH', 'SPEL', 'LLM')),
    CONSTRAINT ck_detect_rule_mode CHECK (execution_mode IN ('BOTH', 'SYNC', 'ASYNC')),
    CONSTRAINT ck_detect_rule_llm_async CHECK (rule_type <> 'LLM' OR execution_mode = 'ASYNC'),
    CONSTRAINT ck_detect_rule_reference CHECK (
        (rule_type = 'MEDIA_HASH' AND media_library_id IS NOT NULL AND spel_release_id IS NULL AND spel_rule_id IS NULL)
        OR (rule_type = 'SPEL' AND media_library_id IS NULL AND spel_release_id IS NOT NULL
            AND spel_rule_id IS NOT NULL AND CHAR_LENGTH(TRIM(spel_rule_id)) > 0)
        OR (rule_type IN ('KEYWORD', 'REGEX', 'LLM') AND media_library_id IS NULL
            AND spel_release_id IS NULL AND spel_rule_id IS NULL)),
    CONSTRAINT ck_detect_rule_action CHECK (
        (rule_type = 'SPEL' AND on_hit_action IS NULL AND risk_labels_json IS NULL AND parameters_json IS NULL)
        OR (rule_type <> 'SPEL' AND on_hit_action IS NOT NULL AND on_hit_action IN ('PASS', 'REVIEW', 'REJECT'))),
    CONSTRAINT ck_detect_rule_labels CHECK (risk_labels_json IS NULL OR JSON_TYPE(risk_labels_json) = 'ARRAY'),
    CONSTRAINT ck_detect_rule_parameters CHECK (parameters_json IS NULL OR JSON_TYPE(parameters_json) = 'OBJECT'),
    CONSTRAINT ck_detect_rule_required_parameters CHECK (rule_type NOT IN ('KEYWORD', 'REGEX', 'LLM') OR parameters_json IS NOT NULL),
    CONSTRAINT ck_detect_rule_state CHECK (enabled IN (0, 1) AND deleted IN (0, 1) AND version > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='场景业务检测规则';
