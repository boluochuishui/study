package org.example.study.baseSdk.log;

public final class LogContextHolder {

    private static final ThreadLocal<LogContext> HOLDER = ThreadLocal.withInitial(LogContext::empty);

    private LogContextHolder() {
    }

    public static LogContext current() {
        return HOLDER.get();
    }

    public static void set(LogContext context) {
        HOLDER.set(context == null ? LogContext.empty() : context);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
