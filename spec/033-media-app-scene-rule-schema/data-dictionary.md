# 数据字典

## 公共字段

以下字段均存在于六张表中，不在后续表格重复列出。字符集采用现有 `utf8mb4`，业务编码与哈希按大小写敏感规则存储。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | 自增内部主键 |
| tenant_id | BIGINT UNSIGNED NOT NULL | 所属租户，不接受客户端指定 |
| enabled | TINYINT(1) NOT NULL DEFAULT 1 | 是否启用 |
| deleted | TINYINT(1) NOT NULL DEFAULT 0 | 是否软删除 |
| version | BIGINT NOT NULL DEFAULT 1 | 乐观锁版本，成功修改时递增 |
| created_by | BIGINT UNSIGNED NOT NULL DEFAULT 0 | 创建人用户 ID，系统为 0 |
| created_at | DATETIME(3) NOT NULL | 创建时间 |
| updated_by | BIGINT UNSIGNED NOT NULL DEFAULT 0 | 更新人用户 ID |
| updated_at | DATETIME(3) NOT NULL | 更新时间 |

创建时更新人/时间与创建人/时间一致；修改时保留创建信息，服务端填充操作人。

## media_library：媒资库

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| library_code | VARCHAR(128) NOT NULL | 租户内唯一库编码 |
| library_name | VARCHAR(128) NOT NULL | 库名称 |
| content_type | VARCHAR(16) NOT NULL | TEXT/IMAGE/AUDIO/VIDEO |
| library_type | VARCHAR(16) NOT NULL | BLOCKLIST/ALLOWLIST/TESTSET |
| description | VARCHAR(512) NULL | 用途说明 |

唯一键 `(tenant_id, library_code)`；唯一引用键 `(tenant_id, id)`；列表索引 `(tenant_id, content_type, library_type, enabled, deleted)`。不维护可能漂移的媒资数量字段，先按关联表查询数量。

## media_asset：媒资

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| asset_name | VARCHAR(255) NOT NULL | 展示名称，不作为内容唯一标识 |
| content_type | VARCHAR(16) NOT NULL | TEXT/IMAGE/AUDIO/VIDEO |
| content_sha256 | CHAR(64) NOT NULL | 内容 SHA-256 十六进制摘要 |
| hash_version | VARCHAR(32) NOT NULL | 哈希前处理版本 |
| storage_status | VARCHAR(16) NOT NULL DEFAULT 'READY' | 样本准备状态 PENDING/READY/FAILED，保留原设计列名，不保存存储位置 |
| description | VARCHAR(512) NULL | 备注 |

唯一键 `(tenant_id, content_type, hash_version, content_sha256)`；唯一引用键 `(tenant_id, id)`；列表索引 `(tenant_id, content_type, created_at, id)`。摘要限定为 64 位小写十六进制。用户要求的六个存储/文件属性列已从设计和 V9 中移除。

## media_asset_library：媒资与媒资库关联

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| library_id | BIGINT UNSIGNED NOT NULL | 媒资库内部 ID |
| asset_id | BIGINT UNSIGNED NOT NULL | 媒资内部 ID |
| labels_json | JSON NULL | 当前库内的标注标签数组 |
| description | VARCHAR(512) NULL | 当前库内的标注说明 |

唯一键 `(tenant_id, library_id, asset_id)`；反向索引 `(tenant_id, asset_id, deleted)`；复合外键分别关联库和媒资的 `(tenant_id, id)`。两者内容类型必须相同。移出库只是软删除关联，不删除媒资正文；重新加入恢复原关联，保留原创建信息。

## detect_app：接入应用

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| app_id | VARCHAR(128) NOT NULL | 外部 App ID，与 X-App-Id 一致，全局唯一 |
| app_name | VARCHAR(128) NOT NULL | 应用名称 |
| credential_ref | VARCHAR(255) NOT NULL | 服务端密钥配置/Secret 引用，禁止明文密钥 |
| description | VARCHAR(512) NULL | 应用用途 |

唯一键 `(app_id)`，使用大小写敏感比较并与鉴权实现一致；唯一引用键 `(tenant_id, id)`；列表索引 `(tenant_id, enabled, deleted)`。密钥轮换发生在凭证管理端，必要时更新引用，不覆盖历史任务的 App ID。App ID 建立后不修改。

## detect_scene：业务场景

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| app_record_id | BIGINT UNSIGNED NOT NULL | App 内部主键，不是外部字符串 App ID |
| scene_code | VARCHAR(128) NOT NULL | 当前 App 内场景编码，对应请求 sceneCode |
| scene_name | VARCHAR(128) NOT NULL | 场景名称 |
| default_language | VARCHAR(32) NULL | 默认语种，请求值缺失时使用 |
| default_region | VARCHAR(16) NULL | 默认地区，请求值缺失时使用 |
| description | VARCHAR(512) NULL | 场景用途 |

唯一键 `(tenant_id, app_record_id, scene_code)`；唯一引用键 `(tenant_id, id)`；列表索引 `(tenant_id, app_record_id, enabled, deleted)`；复合外键关联 App 的 `(tenant_id, id)`。场景支持的类型由有效检测规则配置决定，不在 JSON 中重复维护另一份类型清单。场景编码建立后不修改。

## detect_rule：业务检测规则

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| scene_id | BIGINT UNSIGNED NOT NULL | 所属业务场景 ID |
| rule_code | VARCHAR(128) NOT NULL | 场景内唯一业务规则编码 |
| rule_name | VARCHAR(128) NOT NULL | 展示名称 |
| content_type | VARCHAR(16) NOT NULL | 一条规则适用一种检测类型 |
| rule_type | VARCHAR(16) NOT NULL | KEYWORD/REGEX/MEDIA_HASH/SPEL/LLM |
| execution_mode | VARCHAR(8) NOT NULL DEFAULT 'BOTH' | BOTH/SYNC/ASYNC，LLM 仅 ASYNC |
| priority | INT NOT NULL DEFAULT 100 | 对应固定节点内规则排序，越小越先执行 |
| on_hit_action | VARCHAR(8) NULL | 非 SPEL 类型必填 PASS/REVIEW/REJECT；SPEL 使用发布快照属性，此列为空 |
| risk_labels_json | JSON NULL | 非 SPEL 类型命中后附加的风险标签数组；SPEL 使用发布快照属性 |
| parameters_json | JSON NULL | 当前引擎的参数对象，按类型验证内容 |
| media_library_id | BIGINT UNSIGNED NULL | MEDIA_HASH 类型必填，其他类型为空 |
| spel_release_id | BIGINT UNSIGNED NULL | SPEL 类型必填，引用已发布快照 |
| spel_rule_id | VARCHAR(128) NULL | SPEL 快照内的字符串规则标识 |
| description | VARCHAR(512) NULL | 规则说明 |

唯一键 `(tenant_id, scene_id, rule_code)`；执行查询索引 `(tenant_id, scene_id, content_type, enabled, deleted, priority, id)`；引用媒资库和 SpEL 发布的列分别建索引。场景、媒资库使用同租户复合外键。

SpEL 表现有主键为 `release_id`，V9 已为 `sdk_spel_rule_release` 增加唯一引用键 `(tenant_id, release_id)`，从新规则表建立复合外键，不修改现有发布数据。数据库外键只能约束发布存在，`spel_rule_id` 是否存在于发布 JSON 中由后续配置服务验证。

参数示例：

```json
{"keywords":["example"],"matchMode":"CONTAINS"}
```

```json
{"pattern":"example","flags":["CASE_INSENSITIVE"]}
```

```json
{"modelRef":"text-risk-model","timeoutMillis":3000}
```

JSON 不允许放密钥、编译后的表达式或任意 Java 类名/Bean 名，这些业务限制需由后续配置服务校验。数据库限定参数为对象、标签为数组。SPEL 参数、动作和新表标签列必须为空，其表达式和属性以所引用的已发布快照为准。KEYWORD/REGEX/LLM 参数对象必填，具体内部键需由后续配置服务校验。

同一场景出现多条相同优先级规则时按内部 ID 稳定排序。未配置有效类型规则或当前模式无可执行规则时按明确的配置缺失路径处理，不能把空规则集合默认为一次成功检测。
