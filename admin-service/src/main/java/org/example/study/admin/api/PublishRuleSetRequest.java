package org.example.study.admin.api;

/** 发布或回滚规则集时携带的乐观锁版本和操作者。 */
public record PublishRuleSetRequest(Long version, String publishedBy) {
}
