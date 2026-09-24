package org.example.study.admin.api;

/** 新建或更新规则集的管理请求。 */
public record SaveRuleSetRequest(String name, String scene, String evaluationMode,
                                 String errorPolicy, Boolean enabled, Long version) {
}
