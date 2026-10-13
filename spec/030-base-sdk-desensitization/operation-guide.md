# 使用说明

依赖 `base-sdk` 的服务可直接调用 `org.example.study.baseSdk.sensitive.SensitiveDataMasker`：

```java
String phone = SensitiveDataMasker.mask("13812345678", SensitiveType.MOBILE_PHONE);
String token = SensitiveDataMasker.mask(rawToken, SensitiveType.SECRET);
String custom = SensitiveDataMasker.mask(userId, 2, 2);

Map<String, Object> safeFields = SensitiveDataMasker.maskFields(
        fields, Map.of("phone", SensitiveType.MOBILE_PHONE,
                       "token", SensitiveType.SECRET));
```

`SECRET` 返回 `[REDACTED]`，不暴露原始长度。自定义方法在保留位数超过实际长度时全遮盖。空串和 `null` 原样返回。

`maskFields` 使用精确字段名，未指定字段保持原值；嵌套 Map 和对象不会自动遍历。输出前应显式列出全部敏感字段；原始请求正文、密钥和二进制内容仍应避免进入日志。本需求没有启用自动日志脱敏。
