package org.example.study.admin.api;

/**
 * 停用配置时的乐观锁版本。
 */
public record VersionRequest(Long version) {
}
