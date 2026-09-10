# 验证记录

## 自动化验证

执行命令：

```text
mvn test
```

执行结果：

```text
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 验证点

- 通用日志事件可以表达日志空间、日志类型、日志级别。
- 业务日志门面会转换为 `BIZ` 类型通用日志事件。
- 责任链日志会输出为 `CHAIN` 类型通用日志事件。
- 现有检测流程不受影响。
