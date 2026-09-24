# 018 设计

## 模块边界

`SpelRuleEngine` 负责校验并编译 `SpelRuleDefinition`，生成不可变的 `SpelRuleSet`。业务热加载模块可以在 `HotLoadModule.compile()` 中调用该引擎，只有全部规则编译成功才切换新快照。

规则定义包含唯一标识、表达式、优先级、启用状态和字符串扩展属性。启用规则按优先级升序、规则标识升序稳定执行；停用规则保留在定义来源中，但不会进入运行时规则集。

## 执行模型

调用方传入根对象和变量表。`FIRST_MATCH` 在第一条命中后停止，`ALL_MATCHES` 执行全部规则。表达式必须返回非空 Boolean；返回其他类型视为规则执行错误。

`FAIL_FAST` 在首个错误处抛出 `SpelRuleEvaluationException`，适合强校验场景。`SKIP_FAILED` 记录 `SpelRuleError` 后继续执行，适合允许单条规则降级的检测场景。结果同时包含命中规则和错误列表，不记录输入内容，避免通用模块泄漏敏感样本。

## 安全边界

运行环境使用只读 `SimpleEvaluationContext`，不注册 BeanResolver、TypeLocator 或方法解析器。编译阶段遍历 SpEL AST，拒绝 Bean 引用、类型引用、构造器、方法、函数、赋值和自增自减节点。

第一版面向可信管理人员配置，不向租户直接开放表达式编辑。`matches` 运算符仍可能产生高成本正则，管理台接入时还需增加规则审核、灰度发布和耗时监控。

## 示例

```java
SpelRuleDefinition rule = new SpelRuleDefinition(
        "text-length", "#contentLength > 100", 10, true, Map.of("action", "REVIEW"));
SpelRuleSet ruleSet = spelRuleEngine.compile(List.of(rule));
SpelRuleEvaluationResult result = ruleSet.evaluate(
        null, Map.of("contentLength", 120), SpelRuleEvaluationMode.ALL_MATCHES,
        SpelRuleErrorPolicy.FAIL_FAST);
```
