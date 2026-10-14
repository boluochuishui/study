package org.example.study.baseSdk.database.api.audit;

/** 数据库写入操作人上下文，不参与租户隔离；系统任务默认使用 0。 */
public final class DatabaseAuditContext {
    private static final ThreadLocal<Long> ACTOR = new ThreadLocal<>();

    private DatabaseAuditContext() {
    }

    public static long actorId() {
        Long actor = ACTOR.get();
        return actor == null ? 0L : actor;
    }

    /** 在当前线程限定操作人范围，关闭后恢复原上下文。异步任务需显式建立自己的范围。 */
    public static Scope open(long actorId) {
        if (actorId < 0) {
            throw new IllegalArgumentException("Audit actor ID must not be negative");
        }
        Long previous = ACTOR.get();
        ACTOR.set(actorId);
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable {
        private final Long previous;
        private boolean closed;

        private Scope(Long previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (!closed) {
                if (previous == null) {
                    ACTOR.remove();
                } else {
                    ACTOR.set(previous);
                }
                closed = true;
            }
        }
    }
}
