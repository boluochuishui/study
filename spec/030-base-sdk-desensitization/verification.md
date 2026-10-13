# 验证记录

## 已执行

- `mvn -pl base-sdk -am -Dtest=SensitiveDataMaskerTests test`：新增 5 例通过。
- `mvn -pl base-sdk -am verify`：SDK 全量 48 例通过，Checkstyle 0 违规，SpotBugs 0 告警。
- 验证覆盖常见敏感类型、整值遮盖、异常格式兜底、Unicode 首尾保留、短值全遮盖、Map 输入不变与返回值不可修改。

## 范围说明

本需求未修改日志、HTTP 响应、数据库数据或其他微服务调用点；不声称现有日志已脱敏。
