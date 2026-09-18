package org.example.study.baseSdk.auth;

import java.util.Optional;

/**
 * 根据请求方法和规范化路径选择唯一鉴权规则。
 */
public interface AuthRuleMatcher {

    Optional<AuthRule> match(String method, String path);

    String currentVersion();
}
