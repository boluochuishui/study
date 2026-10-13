CREATE TABLE IF NOT EXISTS admin_online_test_record
(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    content_type VARCHAR(16) NOT NULL,
    execution_mode VARCHAR(8) NOT NULL,
    app_id VARCHAR(128) NOT NULL,
    scene_code VARCHAR(128) NOT NULL,
    item_count INT NOT NULL,
    task_id VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    result_json TEXT NULL,
    duration_ms BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_online_test_tenant_time (tenant_id, created_at, id),
    KEY idx_online_test_tenant_task (tenant_id, task_id),
    CONSTRAINT fk_online_test_tenant FOREIGN KEY (tenant_id) REFERENCES admin_tenant(id),
    CONSTRAINT fk_online_test_user FOREIGN KEY (user_id) REFERENCES admin_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO admin_permission (permission_code, permission_name)
VALUES ('TEST_READ', 'Read online test records'),
       ('TEST_RUN', 'Run online tests')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT IGNORE INTO admin_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM admin_role r JOIN admin_permission p
WHERE r.role_code = 'SUPER_ADMIN' AND p.permission_code IN ('TEST_READ', 'TEST_RUN');
