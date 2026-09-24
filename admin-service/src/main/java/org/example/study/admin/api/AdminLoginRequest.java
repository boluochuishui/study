package org.example.study.admin.api;

/** 管理台租户用户登录请求。 */
public record AdminLoginRequest(String tenantCode, String username, String password) {
}
