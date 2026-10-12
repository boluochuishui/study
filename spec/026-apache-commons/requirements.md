# 026 Apache Commons 通用工具依赖

## 需求

统一引入字符串、对象、集合与 Map 的 Apache Commons 工具，避免借用日志框架内部工具。版本由父 POM 管理，base-sdk 声明公共依赖，供现有下游模块传递使用。

## 验收标准

- 支持 StringUtils.isBlank、Strings.CI.equals、ObjectUtils、CollectionUtils 和 MapUtils。
- 将基础模块既有 Log4j Strings 和 Spring 集合判空导包替换为相应 Commons API。
- 保留 JDK Objects.requireNonNull、确定非空对象的原生操作和安全令牌恒定时间比较。
- 不增加自定义工具包装类，不批量修改业务模块。
