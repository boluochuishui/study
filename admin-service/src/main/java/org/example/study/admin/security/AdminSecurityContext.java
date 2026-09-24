package org.example.study.admin.security;

/** 管理台请求身份上下文，只能由认证过滤器设置。 */
public final class AdminSecurityContext {

    private static final ThreadLocal<AdminPrincipal> CURRENT = new ThreadLocal<>();

    private AdminSecurityContext() {
    }

    static void set(AdminPrincipal principal) {
        CURRENT.set(principal);
    }

    static void clear() {
        CURRENT.remove();
    }

    public static AdminPrincipal requirePrincipal() {
        AdminPrincipal principal = CURRENT.get();
        if (principal == null) {
            throw new IllegalStateException("Admin principal is unavailable");
        }
        return principal;
    }
}
