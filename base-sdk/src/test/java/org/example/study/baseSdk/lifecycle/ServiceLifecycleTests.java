package org.example.study.baseSdk.lifecycle;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 验证上线检查、摘流竞态、在途等待和超时边界。 */
class ServiceLifecycleTests {
    @Test
    void rejectsBeforeReadyAndAfterDrainAndClosesLeaseOnce() {
        var lifecycle = manager(List.of(), Duration.ofSeconds(1));
        assertThatThrownBy(lifecycle::acquire).isInstanceOf(ServiceUnavailableException.class);
        lifecycle.ready();
        var lease = lifecycle.acquire();
        assertThat(lifecycle.inFlight()).isEqualTo(1);
        lifecycle.beginDrain();
        assertThatThrownBy(lifecycle::acquire).isInstanceOf(ServiceUnavailableException.class);
        lease.close();
        lease.close();
        assertThat(lifecycle.inFlight()).isZero();
    }

    @Test
    void failedStartupDoesNotExposeReadyService() {
        LifecycleParticipant failure = new LifecycleParticipant() {
            @Override
            public void prepare() {
                throw new IllegalStateException("Snapshot is missing");
            }
        };
        var lifecycle = manager(List.of(failure), Duration.ofSeconds(1));
        assertThatThrownBy(lifecycle::ready).hasMessage("Snapshot is missing");
        assertThat(lifecycle.state()).isEqualTo(ServiceState.DRAINING);
        assertThatThrownBy(lifecycle::acquire).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void waitsForAcceptedWorkBeforeClosingResources() throws Exception {
        var draining = new CountDownLatch(1);
        var cleanup = new AtomicInteger();
        LifecycleParticipant participant = new LifecycleParticipant() {
            @Override
            public void onDrain() {
                draining.countDown();
            }

            @Override
            public void afterDrain(Duration remaining) {
                cleanup.incrementAndGet();
            }
        };
        var lifecycle = manager(List.of(participant), Duration.ofSeconds(2));
        lifecycle.start();
        lifecycle.ready();
        var lease = lifecycle.acquire();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var stopping = executor.submit(() -> lifecycle.stop());
            assertThat(draining.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(stopping.isDone()).isFalse();
            assertThat(cleanup.get()).isZero();
            lease.close();
            stopping.get(1, TimeUnit.SECONDS);
        } finally {
            lease.close();
        }
        assertThat(lifecycle.state()).isEqualTo(ServiceState.STOPPED);
        assertThat(cleanup.get()).isEqualTo(1);
        lifecycle.stop();
        assertThat(cleanup.get()).isEqualTo(1);
    }

    @Test
    void timesOutWithoutWaitingForeverAndContinuesAfterHookFailure() {
        AtomicInteger cleanup = new AtomicInteger();
        LifecycleParticipant failure = new LifecycleParticipant() {
            @Override
            public void onDrain() {
                throw new IllegalStateException("Registry unavailable");
            }

            @Override
            public void afterDrain(Duration remaining) {
                cleanup.incrementAndGet();
                assertThat(remaining).isEqualTo(Duration.ZERO);
            }
        };
        var lifecycle = manager(List.of(failure), Duration.ofMillis(30));
        lifecycle.ready();
        try (var lease = lifecycle.acquire()) {
            long started = System.nanoTime();
            lifecycle.stop();
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(1));
            assertThat(lifecycle.state()).isEqualTo(ServiceState.STOPPED);
            assertThat(cleanup.get()).isEqualTo(1);
        }
    }

    private ServiceLifecycleManager manager(List<LifecycleParticipant> participants, Duration timeout) {
        var properties = new LifecycleProperties();
        properties.setDrainTimeout(timeout);
        return new ServiceLifecycleManager(participants, properties);
    }
}
