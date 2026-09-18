# 设计说明

## 模块结构

```text
baseSdk.database
├── api
│   ├── exception
│   ├── model
│   └── service
└── mybatis
    ├── config
    ├── entity
    ├── mapper
    └── service
```

`api` 是未来拆分 SDK 时的稳定调用边界，不包含 MyBatis 类型；`mybatis` 是当前直连 MySQL 的实现。业务模块调用 `ConfigDataService`，不能直接注入 `ConfigItemMapper`。

## 调用关系

```text
业务模块
    -> ConfigDataService
        -> ConfigDataServiceImpl
            -> ConfigItemMapper
                -> ConfigItemMapper.xml
                    -> MySQL
```

## SQL 文件管理

```text
src/main/resources
├── db
│   ├── bootstrap
│   │   └── 000_create_database.sql
│   └── migration
│       ├── V1__create_sdk_config_item.sql
│       └── V2__normalize_boolean_column_types.sql
└── mapper
    └── baseSdk
        └── ConfigItemMapper.xml
```

- `bootstrap` 用于首次创建数据库，由运维或本地初始化执行，不进入 Flyway版本历史。
- `migration` 使用 Flyway 命名规则，负责表结构和基础数据演进。
- 已执行的迁移不可修改；所有变更新增更高版本 SQL。
- `mapper` 保存自定义查询 SQL，基础单表 CRUD 使用 MyBatis-Plus `BaseMapper`。
- 禁止在 Java 注解或 Service 中拼接 DDL 和版本迁移 SQL。

## 通用配置表

`sdk_config_item` 使用 `namespace + config_key` 唯一约束，包含配置值、启用状态、乐观锁版本、逻辑删除和审计时间。

第一版对外能力：

```java
Optional<ConfigItemData> findEnabled(String namespace, String configKey);

List<ConfigItemData> listEnabled(String namespace);

ConfigItemData save(SaveConfigItemCommand command);

boolean disable(String namespace, String configKey);
```

配置值使用字符串存储，可以承载 JSON、SpEL 或普通文本。具体业务 SDK 应在调用边界完成格式校验，不由通用数据库层解释内容。

## MyBatis-Plus 配置

- `BaseMapper`：基础新增和按主键更新。
- `OptimisticLockerInnerInterceptor`：根据 `version` 防止并发覆盖。
- `PaginationInnerInterceptor`：MySQL 分页，单页最大 500 条。
- `@TableLogic`：逻辑删除。
- `MetaObjectHandler`：自动填充创建时间和更新时间。
- 自定义查询：XML 中显式限制 `deleted=0`、启用状态和最大返回数量。

## 数据库配置

通用配置从环境变量读取：

```text
MYSQL_HOST
MYSQL_PORT
MYSQL_DATABASE
MYSQL_USERNAME
MYSQL_PASSWORD
MYSQL_MIGRATION_ENABLED
```

本机 `application-local.yaml` 保存开发密码并由 `.gitignore` 排除；仓库只保留不含密码的 `application-local.example.yaml`。

生产环境不应使用 `root`，应创建最小权限账号。Flyway迁移账号与应用运行账号后续可以继续拆分。

## 后续拆分

后续可以拆成：

```text
config-data-sdk
    -> model、service 接口

config-data-mybatis-starter
    -> entity、mapper、service 实现、自动配置、SQL 资源
```

再往后如果演进为配置中心服务，可以保留 API 接口并用远程客户端替换 MyBatis 实现。
