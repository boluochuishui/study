package org.example.study.baseSdk.lifecycle;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/** 统一控制准入和在途计数，在 Spring 销毁资源前完成限时收尾。 */
public class ServiceLifecycleManager implements SmartLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLifecycleManager.class);
    private final Object monitor = new Object();
    private final List<LifecycleParticipant> participants;
    private final Duration drainTimeout;
    private final AtomicBoolean shutdownStarted = new AtomicBoolean();
    private volatile ServiceState state = ServiceState.STARTING;
    private volatile boolean running;
    private int inFlight;

    public ServiceLifecycleManager(List<LifecycleParticipant> participants, LifecycleProperties properties) {
        this.participants = List.copyOf(participants);
        this.drainTimeout = properties.getDrainTimeout();
        if (drainTimeout == null || drainTimeout.isNegative() || drainTimeout.isZero()) {
            throw new IllegalArgumentException("Drain timeout must be positive");
        }
    }

    /** 排在业务启动加载之后，所有检查完成才开放入口。 */
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void ready() {
        synchronized (monitor) {
            if (state != ServiceState.STARTING) {
                return;
            }
            try {
                participants.forEach(LifecycleParticipant::prepare);
                state = ServiceState.READY;
                participants.forEach(LifecycleParticipant::onReady);
                LOGGER.info("SERVICE_READY");
            } catch (RuntimeException exception) {
                state = ServiceState.DRAINING;
                LOGGER.error("SERVICE_START_FAILED", exception);
                drainParticipants();
                throw exception;
            }
        }
    }

    /** 状态检查与计数在同一把锁内，避免摘流与新任务接收之间的竞态。 */
    public WorkLease acquire() {
        synchronized (monitor) {
            if (state != ServiceState.READY) {
                throw new ServiceUnavailableException(state);
            }
            inFlight++;
            return new WorkLease();
        }
    }

    public void beginDrain() {
        synchronized (monitor) {
            if (state == ServiceState.DRAINING || state == ServiceState.STOPPED) {
                return;
            }
            state = ServiceState.DRAINING;
            LOGGER.info("SERVICE_DRAINING, inFlight={}", inFlight);
        }
        drainParticipants();
    }

    private void drainParticipants() {
        for (LifecycleParticipant participant : participants) {
            try {
                participant.onDrain();
            } catch (RuntimeException exception) {
                LOGGER.error("SERVICE_DRAIN_HOOK_FAILED, participant={}", participant.getClass().getName(), exception);
            }
        }
    }

    public ServiceState state() {
        synchronized (monitor) {
            return state;
        }
    }

    public int inFlight() {
        synchronized (monitor) {
            return inFlight;
        }
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    @EventListener(ContextClosedEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void stop() {
        if (!shutdownStarted.compareAndSet(false, true)) {
            return;
        }
        long deadline = System.nanoTime() + drainTimeout.toNanos();
        beginDrain();
        synchronized (monitor) {
            while (inFlight > 0) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    LOGGER.warn("SERVICE_DRAIN_TIMEOUT, inFlight={}", inFlight);
                    break;
                }
                try {
                    TimeUnit.NANOSECONDS.timedWait(monitor, remaining);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    LOGGER.warn("SERVICE_DRAIN_INTERRUPTED, inFlight={}", inFlight);
                    break;
                }
            }
        }
        for (LifecycleParticipant participant : participants) {
            try {
                participant.afterDrain(Duration.ofNanos(Math.max(0, deadline - System.nanoTime())));
            } catch (RuntimeException exception) {
                LOGGER.error("SERVICE_STOP_HOOK_FAILED, participant={}", participant.getClass().getName(), exception);
            }
        }
        state = ServiceState.STOPPED;
        running = false;
        LOGGER.info("SERVICE_STOPPED, unfinishedTasks={}", inFlight());
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    /** 每个任务独立持有租约，重复关闭不会造成计数减成负数。 */
    public final class WorkLease implements AutoCloseable {
        private final AtomicBoolean closed = new AtomicBoolean();

        private WorkLease() {
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                synchronized (monitor) {
                    inFlight--;
                    monitor.notifyAll();
                }
            }
        }
    }
}
