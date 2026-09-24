CREATE TABLE IF NOT EXISTS sdk_spel_rule_set
(
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_set_code     VARCHAR(128)    NOT NULL COMMENT '规则集编码',
    rule_set_name     VARCHAR(128)    NOT NULL COMMENT '规则集名称',
    scene             VARCHAR(32)     NOT NULL COMMENT '业务场景',
    evaluation_mode   VARCHAR(32)     NOT NULL COMMENT '命中收集模式',
    error_policy      VARCHAR(32)     NOT NULL COMMENT '规则错误策略',
    published_version BIGINT          NOT NULL DEFAULT 0 COMMENT '当前发布版本',
    version           BIGINT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    enabled           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    deleted           TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    created_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_spel_rule_set_code (rule_set_code),
    KEY idx_spel_rule_set_scene (scene, enabled)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'SpEL 规则集';

CREATE TABLE IF NOT EXISTS sdk_spel_rule_draft
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_set_id     BIGINT UNSIGNED NOT NULL COMMENT '规则集主键',
    rule_id         VARCHAR(128)    NOT NULL COMMENT '规则标识',
    rule_name       VARCHAR(128)    NOT NULL COMMENT '规则名称',
    expression_text VARCHAR(2048)   NOT NULL COMMENT 'SpEL 表达式源码',
    priority        INT             NOT NULL DEFAULT 100 COMMENT '执行优先级',
    attributes_json LONGTEXT        NOT NULL COMMENT '扩展属性 JSON',
    version         BIGINT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    enabled         TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    deleted         TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_spel_rule_draft (rule_set_id, rule_id),
    KEY idx_spel_rule_draft_order (rule_set_id, priority, rule_id),
    CONSTRAINT fk_spel_rule_draft_set FOREIGN KEY (rule_set_id) REFERENCES sdk_spel_rule_set (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'SpEL 规则草稿';

CREATE TABLE IF NOT EXISTS sdk_spel_rule_release
(
    release_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '发布主键及热加载游标',
    rule_set_code   VARCHAR(128)    NOT NULL COMMENT '规则集编码',
    release_version BIGINT          NOT NULL COMMENT '规则集发布版本',
    scene           VARCHAR(32)     NOT NULL COMMENT '业务场景',
    snapshot_json   LONGTEXT        NOT NULL COMMENT '完整规则快照 JSON',
    checksum        CHAR(64)        NOT NULL COMMENT '快照 SHA-256',
    deleted         TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '规则集删除标识',
    published_by    VARCHAR(128)    NOT NULL COMMENT '发布人',
    published_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发布时间',
    PRIMARY KEY (release_id),
    UNIQUE KEY uk_spel_rule_release_version (rule_set_code, release_version),
    KEY idx_spel_rule_release_scene_cursor (scene, release_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'SpEL 规则不可变发布快照';
