# 操作说明

本机 `localhost:3306/content_risk` 已执行 `V9__create_media_app_scene_rule_tables.sql`。IDEA 在该库中刷新表结构可查看 `media_library`、`media_asset`、`media_asset_library`、`detect_app`、`detect_scene`、`detect_rule`。

其他环境先完成 V1 到 V8，再执行 `config-data/src/main/resources/db/migration/V9__create_media_app_scene_rule_tables.sql`。V9 是一次性迁移，包含对 SpEL 发布表新增唯一键，不要在已执行的环境重复运行。MySQL DDL 不支持整份脚本事务回滚；部分失败时先核对已创建表/索引，再继续剩余语句。

验证脚本为 `config-data/src/test/scripts/Test-BusinessSchema.ps1`，连接凭证通过当前进程 `MYSQL_PWD` 传入，禁止将实际密码写进测试脚本。测试在各自事务中插入并回滚；失败案例通过外键、唯一键、CHECK 错误确认约束生效。

数据库已限制同租户引用、基础枚举、JSON 形状、哈希格式和 LLM 异步模式。库与媒资内容类型一致、SpEL 快照内规则存在、库用途与处置动作匹配、对象启用状态等跨表业务校验仍由后续配置服务实施。六张新表当前为空，不自动将已有配置迁入新表。
