DELETE FROM sdk_config_item
WHERE namespace = 'integration-test'
  AND config_key = 'database-sdk';
