param(
    [string]$MysqlExecutable = 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe',
    [string]$HostName = '127.0.0.1',
    [int]$Port = 3306,
    [string]$Database = 'content_risk',
    [string]$Username = 'root'
)

# 密码通过 MYSQL_PWD 注入；每个测试独立事务，结束或报错断开连接时回滚。
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

function Invoke-SchemaCase {
    param([string]$Name, [string]$Sql, [string]$ExpectedError = '')
    $output = & $MysqlExecutable --host=$HostName --port=$Port --user=$Username `
        --database=$Database --default-character-set=utf8mb4 --batch --skip-column-names `
        --execute=$Sql 2>&1
    $exitCode = $LASTEXITCODE
    $result = ($output | ForEach-Object { $_.ToString() }) -join "`n"
    if ($ExpectedError) {
        if ($exitCode -eq 0 -or $result -notmatch $ExpectedError) {
            throw "Schema case failed: $Name. Expected $ExpectedError; received $result"
        }
    } elseif ($exitCode -ne 0 -or $result.Trim() -ne 'OK') {
        throw "Schema case failed: $Name. Received $result"
    }
    Write-Output "PASS: $Name"
}

$fixture = @'
START TRANSACTION;
SET @run_id = UUID();
INSERT INTO admin_tenant (tenant_code, tenant_name) VALUES (CONCAT('schema-a-', @run_id), 'Schema test A');
SET @tenant_a = LAST_INSERT_ID();
INSERT INTO admin_tenant (tenant_code, tenant_name) VALUES (CONCAT('schema-b-', @run_id), 'Schema test B');
SET @tenant_b = LAST_INSERT_ID();
INSERT INTO media_library (tenant_id, library_code, library_name, content_type, library_type)
VALUES (@tenant_a, 'block-text', 'Schema test library', 'TEXT', 'BLOCKLIST');
SET @library_a = LAST_INSERT_ID();
INSERT INTO media_library (tenant_id, library_code, library_name, content_type, library_type)
VALUES (@tenant_b, 'block-text', 'Schema test library B', 'TEXT', 'BLOCKLIST');
SET @library_b = LAST_INSERT_ID();
INSERT INTO media_asset (tenant_id, asset_name, content_type, content_sha256, hash_version)
VALUES (@tenant_a, 'Schema test asset', 'TEXT', SHA2('schema-test-content', 256), 'text-normalize-v1');
SET @asset_a = LAST_INSERT_ID();
INSERT INTO media_asset_library (tenant_id, library_id, asset_id, labels_json)
VALUES (@tenant_a, @library_a, @asset_a, JSON_ARRAY('schema-test'));
INSERT INTO detect_app (tenant_id, app_id, app_name, credential_ref)
VALUES (@tenant_a, CONCAT('schema-app-', @run_id), 'Schema test app', 'test-only-ref');
SET @app_a = LAST_INSERT_ID();
INSERT INTO detect_scene (tenant_id, app_record_id, scene_code, scene_name)
VALUES (@tenant_a, @app_a, 'comment', 'Schema test scene');
SET @scene_a = LAST_INSERT_ID();
INSERT INTO detect_rule (tenant_id, scene_id, rule_code, rule_name, content_type, rule_type, on_hit_action, media_library_id)
VALUES (@tenant_a, @scene_a, 'sample-hash', 'Schema test rule', 'TEXT', 'MEDIA_HASH', 'REJECT', @library_a);
'@

Invoke-SchemaCase 'Six tables and valid relationships' ($fixture + @'
SELECT IF(
    (SELECT COUNT(*) FROM detect_rule WHERE tenant_id = @tenant_a) = 1
    AND (SELECT COUNT(*) FROM media_asset_library WHERE tenant_id = @tenant_a) = 1,
    'OK', 'FAIL');
ROLLBACK;
'@)

Invoke-SchemaCase 'Same content permitted in another tenant' ($fixture + @'
INSERT INTO media_asset (tenant_id, asset_name, content_type, content_sha256, hash_version)
VALUES (@tenant_b, 'Schema test asset B', 'TEXT', SHA2('schema-test-content', 256), 'text-normalize-v1');
SELECT 'OK';
ROLLBACK;
'@)

Invoke-SchemaCase 'Cross-tenant library rejected' ($fixture + @'
INSERT INTO media_asset_library (tenant_id, library_id, asset_id)
VALUES (@tenant_a, @library_b, @asset_a);
'@) 'fk_media_asset_library_library'

Invoke-SchemaCase 'Duplicate membership rejected' ($fixture + @'
INSERT INTO media_asset_library (tenant_id, library_id, asset_id)
VALUES (@tenant_a, @library_a, @asset_a);
'@) 'uk_media_asset_library_pair'

Invoke-SchemaCase 'Duplicate content rejected within tenant' ($fixture + @'
INSERT INTO media_asset (tenant_id, asset_name, content_type, content_sha256, hash_version)
VALUES (@tenant_a, 'Duplicate asset', 'TEXT', SHA2('schema-test-content', 256), 'text-normalize-v1');
'@) 'uk_media_asset_hash'

Invoke-SchemaCase 'App ID globally unique' ($fixture + @'
INSERT INTO detect_app (tenant_id, app_id, app_name, credential_ref)
VALUES (@tenant_b, CONCAT('schema-app-', @run_id), 'Duplicate app', 'test-only-ref');
'@) 'uk_detect_app_app_id'

Invoke-SchemaCase 'Cross-tenant scene reference rejected' ($fixture + @'
INSERT INTO detect_rule (tenant_id, scene_id, rule_code, rule_name, content_type, rule_type, on_hit_action, parameters_json)
VALUES (@tenant_b, @scene_a, 'foreign-scene', 'Invalid rule', 'TEXT', 'KEYWORD', 'REJECT', JSON_OBJECT());
'@) 'fk_detect_rule_scene'

Invoke-SchemaCase 'Synchronous LLM rejected' ($fixture + @'
INSERT INTO detect_rule (tenant_id, scene_id, rule_code, rule_name, content_type, rule_type, execution_mode, on_hit_action, parameters_json)
VALUES (@tenant_a, @scene_a, 'sync-llm', 'Invalid LLM rule', 'TEXT', 'LLM', 'SYNC', 'REVIEW', JSON_OBJECT());
'@) 'ck_detect_rule_llm_async'

Invoke-SchemaCase 'Missing SpEL reference rejected' ($fixture + @'
INSERT INTO detect_rule (tenant_id, scene_id, rule_code, rule_name, content_type, rule_type)
VALUES (@tenant_a, @scene_a, 'missing-spel', 'Invalid SpEL rule', 'TEXT', 'SPEL');
'@) 'ck_detect_rule_reference'

Invoke-SchemaCase 'Non-array labels rejected' ($fixture + @'
UPDATE media_asset_library SET labels_json = JSON_OBJECT('label', 'invalid')
WHERE tenant_id = @tenant_a AND library_id = @library_a AND asset_id = @asset_a;
'@) 'ck_media_asset_library_labels'

Invoke-SchemaCase 'Invalid SHA-256 rejected' ($fixture + @'
INSERT INTO media_asset (tenant_id, asset_name, content_type, content_sha256, hash_version)
VALUES (@tenant_a, 'Invalid hash asset', 'TEXT', 'invalid-hash', 'text-normalize-v1');
'@) 'ck_media_asset_hash'

Invoke-SchemaCase 'All six tables audited and removed fields absent' @'
SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME IN ('media_library','media_asset','media_asset_library','detect_app','detect_scene','detect_rule')
       AND COLUMN_NAME IN ('tenant_id','created_by','created_at','updated_by','updated_at') AND IS_NULLABLE = 'NO') = 30
    AND (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'media_asset'
           AND COLUMN_NAME IN ('storage_ref','mime_type','size_bytes','duration_ms','width','height')) = 0,
    'OK', 'FAIL');
'@
