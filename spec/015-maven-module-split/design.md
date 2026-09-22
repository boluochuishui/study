# 015 设计

## Maven 结构

`study` 为 `pom` 聚合/父工程，统一声明 Java 版本、第三方依赖版本和 Checkstyle、SpotBugs、JaCoCo、Spring Boot 等插件配置。三个子模块使用同一父版本，Maven Reactor 按依赖排序构建。

| 模块 | 类型 | 职责 |
| --- | --- | --- |
| `base-sdk` | 普通 Jar | 责任链、日志、线程池、常量、热加载框架 |
| `config-data` | 普通 Jar | 配置数据 MyBatis 持久化、SQL 迁移 |
| `detect-service` | Spring Boot 可执行 Jar | HTTP 接口、鉴权、检测流程、MQ 适配 |

依赖方向为 `detect-service -> base-sdk`、`detect-service -> config-data`。基础 SDK 不依赖数据库模块。现有鉴权实现暂留在检测服务，因为其过滤器依赖 API 错误响应，配置源依赖数据库模块；后续独立鉴权 SDK 时再解耦。数据库服务集成测试暂留检测服务，因为测试使用鉴权相关配置及应用上下文。

保持 Java 包名和运行配置不变，应用仍以 `org.example.study.StudyApplication` 扫描依赖模块中的 Spring Bean。`config-data` Jar 携带 `mapper` XML 和 Flyway SQL，通过原有 classpath 配置加载。没有业务模块接入热加载，原有 `014` 未提交改动仅随文件迁移。

CI 在仓库根执行 `mvn clean verify`，各模块质量报告分别上传。手动发布将版本写入所有 Reactor POM，上传 `detect-service` 可执行 Jar，并用该 Jar 构建 Docker 镜像。

## 后续边界

管理台作为独立应用再新增模块；当前不复用检测服务的 HTTP 鉴权实现。需要复用该能力时，先移除对检测 API 的反向依赖，并定义独立自动配置或显式导入方式。音视文图拆为 RPC 服务时，再按服务契约抽取检测领域模型。

