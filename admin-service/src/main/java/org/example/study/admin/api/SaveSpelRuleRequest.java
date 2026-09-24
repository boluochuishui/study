package org.example.study.admin.api;

import java.util.Map;

/** 新建或更新单条 SpEL 规则草稿的管理请求。 */
public record SaveSpelRuleRequest(String name, String expression, Integer priority,
                                  Boolean enabled, Map<String, String> attributes, Long version) {

    public SaveSpelRuleRequest {
        attributes = attributes == null ? null : Map.copyOf(attributes);
    }
}
