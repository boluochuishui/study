INSERT INTO admin_permission (permission_code, permission_name)
VALUES ('LOG_READ', 'Read detection logs')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT IGNORE INTO admin_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM admin_role r
JOIN admin_permission p ON p.permission_code = 'LOG_READ'
WHERE r.role_code = 'SUPER_ADMIN';
