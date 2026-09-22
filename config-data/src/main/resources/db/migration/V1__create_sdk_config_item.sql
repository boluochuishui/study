CREATE TABLE IF NOT EXISTS sdk_config_item
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    namespace    VARCHAR(128)    NOT NULL COMMENT '配置命名空间',
    config_key   VARCHAR(128)    NOT NULL COMMENT '配置键',
    config_value LONGTEXT        NOT NULL COMMENT '配置值',
    version      BIGINT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    enabled      TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    description  VARCHAR(512)    NULL COMMENT '配置说明',
    deleted      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_namespace_config_key (namespace, config_key),
    KEY idx_namespace_enabled (namespace, enabled)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'SDK 通用配置项';
