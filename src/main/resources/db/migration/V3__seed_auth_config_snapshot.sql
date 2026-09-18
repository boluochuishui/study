INSERT IGNORE INTO sdk_config_item
    (namespace, config_key, config_value, version, enabled, description, deleted)
VALUES
    ('system-auth',
     'published-rules',
     '{"version":"db-v1","publishedAt":"2026-09-18T00:00:00Z","publishedBy":"system","rules":[{"ruleId":"detect-sync","name":"同步检测","pathPattern":"/api/detect/sync","methods":["POST"],"authMode":"SIGNATURE","options":{"requiredPermission":"detect:sync"},"priority":10,"enabled":true},{"ruleId":"detect-async","name":"异步检测","pathPattern":"/api/detect/async","methods":["POST"],"authMode":"SIGNATURE","options":{"requiredPermission":"detect:async"},"priority":20,"enabled":true},{"ruleId":"health-liveness","name":"存活检查","pathPattern":"/actuator/health/liveness","methods":["GET"],"authMode":"ANONYMOUS","options":{},"priority":100,"enabled":true},{"ruleId":"health-readiness","name":"就绪检查","pathPattern":"/actuator/health/readiness","methods":["GET"],"authMode":"ANONYMOUS","options":{},"priority":110,"enabled":true}],"checksum":"bootstrap"}',
     1,
     1,
     '系统级鉴权首个发布快照',
     0);
