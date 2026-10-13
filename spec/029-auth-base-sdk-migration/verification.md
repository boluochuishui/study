# 验证记录

## 已执行

- `mvn -pl access-service -am -DskipTests compile`：五个 Reactor 模块编译通过，Checkstyle 通过。
- 定向测试：SDK 自动装配、规则匹配、签名共 6 例通过；接入服务 HTTP 鉴权 2 例通过。
- 前一次定向测试包含检测 HTTP 集成测试 5 例，通过；未启动检测 RPC 服务时，预期的降级日志会出现。
- `mvn -pl access-service -am -DskipTests clean package`：干净打包通过。`base-sdk` Jar 包含鉴权过滤器和自动配置；接入服务 Jar 仅包含自身的规则及数据库适配器，不包含旧 `baseSdk.auth` 类。
- `git diff --check` 无空白错误。

## 未覆盖

数据库集成测试需显式启用本地 MySQL；本需求未运行全部模块的完整测试与 SpotBugs/JaCoCo 校验。
