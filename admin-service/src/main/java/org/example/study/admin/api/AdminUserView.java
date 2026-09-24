package org.example.study.admin.api;

/** 不包含密码摘要的管理用户视图。 */
public record AdminUserView(long id, long tenantId, String username, String displayName, boolean enabled) {
}
