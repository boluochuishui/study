# 设计

## 字段约定

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| created_by | BIGINT UNSIGNED NOT NULL DEFAULT 0 | 创建人用户 ID |
| created_at | DATETIME(3) NOT NULL | 创建时间 |
| updated_by | BIGINT UNSIGNED NOT NULL DEFAULT 0 | 最近更新人用户 ID |
| updated_at | DATETIME(3) NOT NULL | 最近更新时间 |

范围为 `admin_tenant`、`admin_user`、`admin_role`、`admin_permission`、`admin_user_role`、`admin_role_permission`、`admin_access_token`、`sdk_config_item`、`sdk_spel_rule_set`、`sdk_spel_rule_draft`、`sdk_spel_rule_release`、`admin_online_test_record`。

## 写入来源

数据库 SDK 提供独立于 HTTP 的 `DatabaseAuditContext`。管理台认证过滤器验证令牌后建立用户操作范围，请求完成或异常时自动清理。没有用户身份的初始化、定时任务记为系统 `0`。该上下文不携带租户，租户仍由现有显式 `tenantId` 参数隔离。异步写入需在执行线程显式建立操作范围，不能依赖线程上下文自动传播。

MyBatis-Plus 实体共享 `AuditedEntity`，自动填充操作人和时间；创建字段禁止进入更新 SQL。更新人和更新时间强制刷新，避免实体中上一次更新信息阻止自动填充。XML 写入通过绑定当前操作人填充审计列。登录时尚未建立管理请求上下文，令牌创建人直接使用已验证的登录用户 ID；在线测试记录使用服务端传入的操作用户 ID。

创建时更新人等于创建人，更新时间等于创建时间。角色更新保留创建信息。现有角色授权和用户角色替换会删除旧关联并插入新关联，新的关联保存本次授权人；四字段不能代替完整删除/历史操作日志，本需求不新增历史审计表。

## 历史数据

保留已有时间字段。无法确定操作人的历史数据统一为 `0`。令牌可从 `user_id` 还原创建人，但不推测历史撤销操作人。在线测试记录为追加记录，创建和更新人从 `user_id` 还原。SpEL 发布的创建和更新时间从 `published_at` 补齐；原 `published_by` 字符串仍保留，不尝试猜测同名用户。没有任何历史时间的关联和权限记录使用迁移时间，并明确此时间不是历史真实操作时间。

## 兼容性

保持现有 API DTO 和请求结构；此需求先完成数据库和写入审计，不新增前端审计展示。已有实体审计字段在 SQL 查询中映射，方便后续展示扩展。迁移后需先部署 V8 再上线新代码，旧代码新增记录仍可使用系统默认值。
