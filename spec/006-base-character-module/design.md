# 设计说明

## 包路径

```text
org.example.study.baseSdk.constant
```

## 类设计

新增 `BaseChars`，作为 baseSdk 中的基础字符常量类。

设计约束：

- 使用 `final class`，禁止继承。
- 构造方法私有化，禁止实例化。
- 常量统一使用 `public static final`。
- 中文注释描述字符用途。

## 常量分组

```text
基础字符串：EMPTY、SPACE、TAB、CR、LF、CRLF
分隔符：COMMA、DOT、COLON、SEMICOLON、SLASH、BACKSLASH、UNDERLINE、HYPHEN、PIPE
括号：LEFT/RIGHT_PARENTHESES、LEFT/RIGHT_BRACKET、LEFT/RIGHT_BRACE
常用符号：AMPERSAND、EQUAL、QUESTION_MARK、ASTERISK、PERCENT、AT、HASH
字符常量：CHAR_SPACE、CHAR_COMMA、CHAR_DOT 等
```

## 使用示例

```java
String key = appId + BaseChars.COLON + taskId;
String[] parts = value.split(BaseChars.COMMA);
```
