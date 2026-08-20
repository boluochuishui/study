package org.example.study.baseSdk.log;

public class LogScope implements AutoCloseable {

    private final LogContext previous;

    private LogScope(LogContext next) {
        this.previous = LogContextHolder.current();
        LogContextHolder.set(next);
    }

    public static LogScope open(LogContext context) {
        return new LogScope(context);
    }

    @Override
    public void close() {
        LogContextHolder.set(previous);
    }
}
