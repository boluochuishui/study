package org.example.study.baseSdk.auth;

/**
 * 当前线程的认证上下文，过滤器必须在 finally 中清理。
 */
public final class AuthenticationContext {

    private static final ThreadLocal<AuthenticationPrincipal> HOLDER = new ThreadLocal<>();

    private AuthenticationContext() {
    }

    public static void set(AuthenticationPrincipal principal) {
        HOLDER.set(principal);
    }

    public static AuthenticationPrincipal get() {
        return HOLDER.get();
    }

    public static AuthenticationPrincipal requirePrincipal() {
        AuthenticationPrincipal principal = HOLDER.get();
        if (principal == null) {
            throw new IllegalStateException("Authentication principal is missing");
        }
        return principal;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
