# 设计说明

## 模块边界

`base-sdk` 依赖 `cn.hutool:hutool-core`，通过 `SensitiveDataMasker` 封装常见类型的脱敏策略。业务仅引用 SDK 的 `SensitiveType` 和 `SensitiveDataMasker`，不需要依赖 Hutool API。

## API

```java
SensitiveDataMasker.mask("13812345678", SensitiveType.MOBILE_PHONE);
SensitiveDataMasker.mask("access-token", SensitiveType.SECRET);
SensitiveDataMasker.mask("A😀BC", 1, 1);
SensitiveDataMasker.maskFields(values, Map.of("phone", SensitiveType.MOBILE_PHONE));
```

支持手机号、邮箱、身份证、银行卡号、中文姓名、IPv4 与整值遮盖。自定义首尾保留按 Unicode 字符计算，待遮盖值过短时全部替换为星号。Hutool 未能改变的非空输入使用 `[REDACTED]` 兜底。

字段策略使用精确字段名。未列入策略的字段原样保留；列入策略的非字符串值整值遮盖；返回 Map 不可修改。调用方不得把未列入策略的敏感字段误认为已经受保护。原对象、嵌套对象及任意文本不会自动扫描。

日志脱敏、异常堆栈处理、输出端统一遮盖和此前发现的日志配置名不一致问题，均不在本需求范围内。
