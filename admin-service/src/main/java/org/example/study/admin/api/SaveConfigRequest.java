package org.example.study.admin.api;

/**
 * 配置项更新请求；新建时版本为 0。
 */
public record SaveConfigRequest(String value, Boolean enabled, String description, Long version) {
}
