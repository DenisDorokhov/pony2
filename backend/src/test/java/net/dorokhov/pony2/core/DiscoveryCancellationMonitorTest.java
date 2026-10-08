package net.dorokhov.pony2.core;

import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

class DiscoveryCancellationMonitorTest {

    @Test
    void shouldCancelIdleGroupAndAllowNewWork() throws Exception {
        DiscoveryCancellationMonitor monitor = new DiscoveryCancellationMonitor();
        monitor.cancelAndWait(Duration.ZERO);
        assertThat(monitor.isCancelled()).isTrue();
        assertThatThrownBy(monitor::interruptIfCancelled).isInstanceOf(DiscoveryInterruptedException.class);
        assertThat(monitor.hasRunningTasks()).isFalse();
        assertThatThrownBy(monitor::taskFinished).isInstanceOf(IllegalStateException.class);
        monitor.taskStarted();
        assertThat(monitor.isCancelled()).isFalse();
        monitor.taskFinished();
    }

    @Test
    void shouldWaitForEveryParallelTaskAndWakeAllWaiters() throws Exception {
        DiscoveryCancellationMonitor monitor = new DiscoveryCancellationMonitor();
        List<CountDownLatch> releases = List.of(new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1));
        try (var executor = Executors.newFixedThreadPool(5)) {
            List<Future<?>> tasks = new ArrayList<>();
            for (CountDownLatch release : releases) {
                monitor.taskStarted();
                tasks.add(executor.submit(() -> {
                    try {
                        assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                        assertThatThrownBy(monitor::interruptIfCancelled).isInstanceOf(DiscoveryInterruptedException.class);
                    } finally {
                        monitor.taskFinished();
                    }
                    return null;
                }));
            }
            List<Future<?>> waiters = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                waiters.add(executor.submit(() -> {
                    monitor.cancelAndWait(Duration.ofSeconds(5));
                    return null;
                }));
            }
            try {
                await().atMost(Duration.ofSeconds(2)).until(monitor::isCancelled);
                for (int i = 0; i < 2; i++) {
                    releases.get(i).countDown();
                    tasks.get(i).get(2, TimeUnit.SECONDS);
                    assertThat(waiters).allSatisfy(waiter -> assertThat(waiter.isDone()).isFalse());
                }
                releases.get(2).countDown();
                tasks.get(2).get(2, TimeUnit.SECONDS);
                for (Future<?> waiter : waiters) {
                    waiter.get(2, TimeUnit.SECONDS);
                }
                assertThat(monitor.isCancelled()).isTrue();
                assertThat(monitor.hasRunningTasks()).isFalse();
            } finally {
                releases.forEach(CountDownLatch::countDown);
            }
        }
    }

    @Test
    void shouldKeepCancellationAfterTimeoutAndCompletion() throws Exception {
        DiscoveryCancellationMonitor monitor = new DiscoveryCancellationMonitor();
        monitor.taskStarted();
        assertThatThrownBy(() -> monitor.cancelAndWait(Duration.ofMillis(20))).isInstanceOf(TimeoutException.class);
        assertThat(monitor.hasRunningTasks()).isTrue();
        assertThatThrownBy(monitor::taskStarted).isInstanceOf(DiscoveryInterruptedException.class);
        monitor.taskFinished();
        monitor.cancelAndWait(Duration.ZERO);
        assertThat(monitor.isCancelled()).isTrue();
    }

    @Test
    void shouldAllowNextGroupOnlyAfterAllOldTasksFinish() {
        DiscoveryCancellationMonitor monitor = new DiscoveryCancellationMonitor();
        monitor.taskStarted();
        assertThatThrownBy(() -> monitor.cancelAndWait(Duration.ZERO)).isInstanceOf(TimeoutException.class);
        assertThatThrownBy(monitor::taskStarted).isInstanceOf(DiscoveryInterruptedException.class);
        monitor.taskFinished();
        assertThat(monitor.isCancelled()).isTrue();
        monitor.taskStarted();
        assertThat(monitor.isCancelled()).isFalse();
        monitor.taskFinished();
    }

    @Test
    void shouldRejectNegativeTimeoutWithoutCancelling() {
        DiscoveryCancellationMonitor monitor = new DiscoveryCancellationMonitor();
        assertThatThrownBy(() -> monitor.cancelAndWait(Duration.ofSeconds(-1))).isInstanceOf(IllegalArgumentException.class);
        assertThat(monitor.isCancelled()).isFalse();
    }

    @Test
    void shouldInterruptWaiterWithoutClearingTaskCancellation() throws Exception {
        DiscoveryCancellationMonitor monitor = new DiscoveryCancellationMonitor();
        monitor.taskStarted();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            try {
                monitor.cancelAndWait(Duration.ofSeconds(5));
            } catch (Exception e) {
                failure.set(e);
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });
        waiter.start();
        try {
            await().atMost(Duration.ofSeconds(2)).until(monitor::isCancelled);
            waiter.interrupt();
            waiter.join(2000);
            assertThat(waiter.isAlive()).isFalse();
            assertThat(failure.get()).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(InterruptedException.class);
            assertThat(interrupted).isTrue();
            assertThat(monitor.isCancelled()).isTrue();
            assertThat(monitor.hasRunningTasks()).isTrue();
        } finally {
            monitor.taskFinished();
            waiter.interrupt();
            waiter.join(2000);
        }
    }
}
