package org.example.study.baseSdk.sensitive;

import cn.hutool.core.util.DesensitizedUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 通用显式脱敏工具，不修改输入对象，也不自动拦截日志或序列化结果。
 */
public final class SensitiveDataMasker {

    public static final String REDACTED = "[REDACTED]";

    private SensitiveDataMasker() {
        throw new UnsupportedOperationException("SensitiveDataMasker cannot be instantiated");
    }

    /** 按已知数据类型脱敏；无法安全处理的非空值整值遮盖。 */
    public static String mask(String value, SensitiveType type) {
        Objects.requireNonNull(type, "Sensitive type must not be null");
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (type == SensitiveType.SECRET) {
            return REDACTED;
        }
        String result = switch (type) {
            case MOBILE_PHONE -> DesensitizedUtil.mobilePhone(value);
            case EMAIL -> DesensitizedUtil.email(value);
            case ID_CARD -> DesensitizedUtil.idCardNum(value, 1, 2);
            case BANK_CARD -> DesensitizedUtil.bankCard(value);
            case CHINESE_NAME -> DesensitizedUtil.chineseName(value);
            case IPV4 -> DesensitizedUtil.ipv4(value);
            case SECRET -> REDACTED;
        };
        return result == null || result.equals(value) ? REDACTED : result;
    }

    /** 按 Unicode 字符数保留首尾，其余位置用星号替换；短值全部遮盖。 */
    public static String mask(String value, int visiblePrefix, int visibleSuffix) {
        if (visiblePrefix < 0 || visibleSuffix < 0) {
            throw new IllegalArgumentException("Visible lengths must not be negative");
        }
        if (value == null || value.isEmpty()) {
            return value;
        }
        int length = value.codePointCount(0, value.length());
        if ((long) visiblePrefix + visibleSuffix >= length) {
            return "*".repeat(length);
        }
        int prefixEnd = value.offsetByCodePoints(0, visiblePrefix);
        int suffixStart = value.offsetByCodePoints(0, length - visibleSuffix);
        return value.substring(0, prefixEnd) + "*".repeat(length - visiblePrefix - visibleSuffix)
                + value.substring(suffixStart);
    }

    /** 只处理策略表列出的字段；非字符串敏感值整值遮盖。 */
    public static Map<String, Object> maskFields(Map<String, ?> values, Map<String, SensitiveType> policies) {
        Objects.requireNonNull(values, "Values must not be null");
        Objects.requireNonNull(policies, "Policies must not be null");
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((name, value) -> {
            if (policies.containsKey(name) && policies.get(name) == null) {
                throw new IllegalArgumentException("Sensitive field policy must not be null");
            }
            SensitiveType type = policies.get(name);
            if (type == null) {
                result.put(name, value);
            } else if (value == null) {
                result.put(name, null);
            } else if (value instanceof String stringValue) {
                result.put(name, mask(stringValue, type));
            } else {
                result.put(name, REDACTED);
            }
        });
        return Collections.unmodifiableMap(result);
    }
}
