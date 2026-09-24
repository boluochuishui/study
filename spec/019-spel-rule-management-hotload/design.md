# 019 设计

## 数据模型

`sdk_spel_rule_set` 保存规则集元数据和当前发布版本；`sdk_spel_rule_draft` 保存管理台草稿；`sdk_spel_rule_release` 保存不可变完整发布快照。规则集和草稿使用乐观锁，发布记录使用自增 `release_id` 作为跨实例热加载游标。

发布快照包含规则集编码、场景、发布版本、执行模式、错误策略、启用状态及全部规则定义。快照保存表达式源码，编译对象只存在于各服务 JVM 内存。

## 发布流程

1. 锁定规则集并校验客户端版本。
2. 读取当前全部草稿规则。
3. 将草稿转换为 `SpelRuleDefinition` 并整批编译。
4. 生成快照 JSON 与 SHA-256 校验和。
5. 插入不可变发布记录并更新规则集发布版本。
6. 任意步骤失败时回滚事务，不产生半成品发布。

回滚会读取历史快照、重新校验并生成新的发布版本，不修改历史记录。

## 热加载流程

检测服务按 `detect.rules.spel.scene` 过滤自身场景。启动全量读取每个规则集的最新发布，增量按 `release_id` 升序读取。`SpelRuleHotLoadModule` 将快照编译成 `SpelRuleCatalog`，再由 `HotLoadManager` 原子替换。

热加载通过 `SPEL_RULE_HOTLOAD_ENABLED=true` 开启，默认关闭，避免未配置数据库的本地服务启动时建立连接。数据库为规则真源，后续可以增加消息通知触发即时刷新，但通知不承载规则内容。

## 管理接口

```text
GET  /api/admin/rule-sets
PUT  /api/admin/rule-sets/{code}
GET  /api/admin/rule-sets/{code}/rules
PUT  /api/admin/rule-sets/{code}/rules/{ruleId}
POST /api/admin/rule-sets/{code}/validate
POST /api/admin/rule-sets/{code}/publish
GET  /api/admin/rule-sets/{code}/releases
POST /api/admin/rule-sets/{code}/rollback/{releaseVersion}
```
