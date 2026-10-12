# 验证记录

## 验收项

- 相同违规文本第二次检测可从 Redis 缓存短路返回。
- Redis Key 和 Value 不包含文本原文。
- 正常与复审结果不写入 Redis。
- 同步模式不调用大模型 SPI。
- 异步模式在开关开启时调用大模型 SPI。
- Redis 和大模型异常不阻断本地检测。

## 执行结果

- `mvn test -q`：通过，覆盖全部 Maven 模块。
- `mvn -pl text-detect-service -am verify -DskipTests -q`：通过。
- Checkstyle：通过。
- SpotBugs：通过。
- 文本归一化、SHA-256 稳定性、规则汇总、违规缓存最小化和异步大模型边界均有单元测试。
- Spring Boot 文本服务上下文测试通过，测试过程不要求 Redis 实例在线。
