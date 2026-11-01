package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.core.ShutdownService;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

class LibraryJobSynchronizerTest {

    private final ShutdownService shutdownService = new ShutdownService();
    private final LibraryJobSynchronizer synchronizer = new LibraryJobSynchronizer(shutdownService);

    @Test
    void shouldContinueDiscoveryWhenNotStopped() {
        assertThatCode(synchronizer::interruptDiscoveryIfStopped).doesNotThrowAnyException();
        assertThat(synchronizer.isDiscoveryCancelled()).isFalse();
    }

    @Test
    void shouldInterruptDiscoveryOnShutdownWithoutCancelling() {
        shutdownService.onApplicationEvent();

        assertThatThrownBy(synchronizer::interruptDiscoveryIfStopped).isInstanceOf(DiscoveryInterruptedException.class);
        assertThatCode(synchronizer::interruptDiscoveryIfCancelled).doesNotThrowAnyException();
        assertThat(synchronizer.isDiscoveryCancelled()).isFalse();
    }

    @Test
    void shouldKeepDiscoveryCancelledAfterStopCheck() {
        synchronizer.cancelDiscovery();

        assertThatThrownBy(synchronizer::interruptDiscoveryIfStopped).isInstanceOf(DiscoveryInterruptedException.class);
        assertThat(synchronizer.isDiscoveryCancelled()).isTrue();
    }

    @Test
    void shouldWaitForCancelledDiscoveryJobAndAllTasks() throws Exception {
        LibraryJobSynchronizer.LibraryJobRegistration discovery = synchronizer.registerDiscoveryJob();
        LibraryJobSynchronizer.DiscoveryTaskRegistration task = synchronizer.registerDiscoveryTask();
        //noinspection TryFinallyCanBeTryWithResources
        try (ExecutorService worker = Executors.newSingleThreadExecutor()) {
            Future<?> cancellation = worker.submit(() -> synchronizer.cancelDiscoveryAndWait(Duration.ofSeconds(3)));
            await().until(synchronizer::isDiscoveryCancelled);
            assertThat(cancellation.isDone()).isFalse();
            discovery.close();
            assertThat(cancellation.isDone()).isFalse();
            task.close();

            cancellation.get(2, TimeUnit.SECONDS);
            assertThat(synchronizer.hasRunningTasks()).isFalse();
        } finally {
            task.close();
            discovery.close();
        }
    }

    @Test
    void shouldFailWhenCancelledDiscoveryDoesNotStop() throws Exception {
        try (LibraryJobSynchronizer.LibraryJobRegistration ignored = synchronizer.registerDiscoveryJob()) {
            assertThatThrownBy(() -> synchronizer.cancelDiscoveryAndWait(Duration.ZERO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasCauseInstanceOf(TimeoutException.class);
            assertThat(synchronizer.isDiscoveryCancelled()).isTrue();
        }
    }

    @Test
    void shouldLeaveScanRunningWhenCancellingDiscovery() throws Exception {
        try (LibraryJobSynchronizer.LibraryJobRegistration ignored = synchronizer.registerScanJob(Duration.ZERO)) {
            synchronizer.cancelDiscoveryAndWait(Duration.ZERO);

            assertBusy();
        }
    }

    @Test
    void shouldNotifyOutsideLockWhenStartingScan() throws Exception {
        LibraryJobSynchronizer.LibraryJobRegistration discovery = synchronizer.registerDiscoveryJob();
        LibraryJobSynchronizer.DiscoveryTaskRegistration task = synchronizer.registerDiscoveryTask();
        //noinspection TryFinallyCanBeTryWithResources
        try (ExecutorService worker = Executors.newSingleThreadExecutor();
             LibraryJobSynchronizer.CancellationSubscription ignoredSubscription = synchronizer.onCancel(() -> {
                 try {
                     worker.submit(() -> {
                         task.close();
                         discovery.close();
                     }).get(2, TimeUnit.SECONDS);
                 } catch (Exception e) {
                     throw new AssertionError(e);
                 }
             });
             LibraryJobSynchronizer.LibraryJobRegistration ignoredScan = synchronizer.registerScanJob(Duration.ofSeconds(3))) {
            assertThat(synchronizer.hasRunningTasks()).isFalse();
            assertBusy();
        } finally {
            task.close();
            discovery.close();
        }
    }

    @Test
    void shouldUnsubscribeIndependentlyAndNotifyOnlyOnce() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Runnable callback = calls::incrementAndGet;
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredDiscovery = synchronizer.registerDiscoveryJob();
             LibraryJobSynchronizer.CancellationSubscription first = synchronizer.onCancel(callback);
             LibraryJobSynchronizer.CancellationSubscription ignoredSecond = synchronizer.onCancel(callback)) {
            first.close();
            first.close();
            synchronizer.cancelDiscovery();
            synchronizer.cancelDiscovery();
            assertThat(calls).hasValue(1);
            assertBusy();
        }
    }

    @Test
    void shouldImmediatelyNotifyLateSubscription() throws Exception {
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredDiscovery = synchronizer.registerDiscoveryJob()) {
            synchronizer.cancelDiscovery();
            AtomicInteger calls = new AtomicInteger();
            try (LibraryJobSynchronizer.CancellationSubscription ignoredSubscription = synchronizer.onCancel(calls::incrementAndGet)) {
                assertThat(calls).hasValue(1);
            }
        }
    }

    @Test
    void shouldClearSubscriptionsWhenJobFinishes() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        LibraryJobSynchronizer.CancellationSubscription subscription;
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredDiscovery = synchronizer.registerDiscoveryJob()) {
            subscription = synchronizer.onCancel(calls::incrementAndGet);
        }
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredNext = synchronizer.registerDiscoveryJob()) {
            synchronizer.cancelDiscovery();
            subscription.close();
            assertThat(calls).hasValue(0);
        }
    }

    @Test
    void shouldContinueNotifyingAfterCallbackFailure() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredDiscovery = synchronizer.registerDiscoveryJob();
             LibraryJobSynchronizer.CancellationSubscription ignoredFirst = synchronizer.onCancel(() -> {
                 throw new IllegalStateException("Test failure");
             });
             LibraryJobSynchronizer.CancellationSubscription ignoredSecond = synchronizer.onCancel(calls::incrementAndGet)) {
            synchronizer.cancelDiscovery();
            assertThat(calls).hasValue(1);
        }
    }

    @Test
    void shouldReleaseFromAnotherThreadOnlyOnce() throws Exception {
        LibraryJobSynchronizer.LibraryJobRegistration first = synchronizer.registerScanJob(Duration.ZERO);
        assertBusy();
        Thread worker = new Thread(first::close);
        worker.start();
        worker.join(5000);
        assertThat(worker.isAlive()).isFalse();

        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredSecond = synchronizer.registerDiscoveryJob()) {
            first.close();
            assertBusy();
        }
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredThird = synchronizer.registerScanJob(Duration.ZERO)) {
            assertBusy();
        }
    }

    @Test
    void shouldReserveScanUntilJobAndAllTasksFinish() throws Exception {
        LibraryJobSynchronizer.LibraryJobRegistration discovery = synchronizer.registerDiscoveryJob();
        LibraryJobSynchronizer.DiscoveryTaskRegistration firstTask = synchronizer.registerDiscoveryTask();
        LibraryJobSynchronizer.DiscoveryTaskRegistration secondTask = synchronizer.registerDiscoveryTask();
        try (ExecutorService callers = Executors.newSingleThreadExecutor()) {
            Future<LibraryJobSynchronizer.LibraryJobRegistration> scan = callers.submit(() -> synchronizer.registerScanJob(Duration.ofSeconds(5)));
            try {
                await().atMost(Duration.ofSeconds(2)).until(synchronizer::isDiscoveryCancelled);
                assertThatThrownBy(synchronizer::registerDiscoveryTask).isInstanceOf(DiscoveryInterruptedException.class);
                assertBusy();
                assertThatThrownBy(() -> {
                    try (LibraryJobSynchronizer.LibraryJobRegistration ignoredUnexpectedRegistration = synchronizer.registerScanJob(Duration.ZERO)) {
                        fail("Expected the operation to reject this request.");
                    }
                })
                        .isInstanceOf(ConcurrentLibraryJobException.class);

                discovery.close();
                firstTask.close();
                firstTask.close();
                assertThat(scan.isDone()).isFalse();
                assertBusy();
                secondTask.close();

                try (LibraryJobSynchronizer.LibraryJobRegistration ignoredJobRegistration = scan.get(2, TimeUnit.SECONDS)) {
                    assertThat(synchronizer.hasRunningTasks()).isFalse();
                    assertBusy();
                    discovery.close();
                    secondTask.close();
                    assertBusy();
                }
                try (LibraryJobSynchronizer.LibraryJobRegistration ignoredNext = synchronizer.registerDiscoveryJob()) {
                    assertThat(synchronizer.isDiscoveryCancelled()).isFalse();
                }
            } finally {
                scan.cancel(true);
                firstTask.close();
                secondTask.close();
                discovery.close();
            }
        }
    }

    @Test
    void shouldKeepJobReservedAfterLastTaskFinishes() throws Exception {
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredDiscovery = synchronizer.registerDiscoveryJob()) {
            try (LibraryJobSynchronizer.DiscoveryTaskRegistration ignoredTask = synchronizer.registerDiscoveryTask()) {
                assertThat(synchronizer.hasRunningTasks()).isTrue();
            }
            assertThat(synchronizer.hasRunningTasks()).isFalse();
            assertBusy();
            try (LibraryJobSynchronizer.DiscoveryTaskRegistration ignoredNextTask = synchronizer.registerDiscoveryTask()) {
                assertThat(synchronizer.hasRunningTasks()).isTrue();
            }
        }
    }

    @Test
    void shouldKeepCancellationAfterTimeoutUntilNextDiscoveryJob() throws Exception {
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredDiscovery = synchronizer.registerDiscoveryJob()) {
            try (LibraryJobSynchronizer.DiscoveryTaskRegistration ignoredTask = synchronizer.registerDiscoveryTask()) {
                assertThatThrownBy(() -> {
                    try (LibraryJobSynchronizer.LibraryJobRegistration ignoredUnexpectedRegistration = synchronizer.registerScanJob(Duration.ofMillis(20))) {
                        fail("Expected the operation to reject this request.");
                    }
                })
                        .isInstanceOf(ConcurrentLibraryJobException.class)
                        .hasMessage("Timed out waiting for discovery to finish.")
                        .hasCauseInstanceOf(TimeoutException.class);
                assertThatThrownBy(synchronizer::interruptDiscoveryIfCancelled).isInstanceOf(DiscoveryInterruptedException.class);
            }
            // Finishing the last task must not let another task reset this job's cancellation.
            assertThatThrownBy(synchronizer::registerDiscoveryTask).isInstanceOf(DiscoveryInterruptedException.class);
        }
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredNext = synchronizer.registerDiscoveryJob();
             LibraryJobSynchronizer.DiscoveryTaskRegistration ignoredTask = synchronizer.registerDiscoveryTask()) {
            assertThat(synchronizer.isDiscoveryCancelled()).isFalse();
        }
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredScan = synchronizer.registerScanJob(Duration.ZERO)) {
            assertBusy();
        }
    }

    @Test
    void shouldCancelQueuedDiscoveryEvenWithoutRegisteredTasks() throws Exception {
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredDiscovery = synchronizer.registerDiscoveryJob()) {
            assertThatThrownBy(() -> {
                try (LibraryJobSynchronizer.LibraryJobRegistration ignoredUnexpectedRegistration = synchronizer.registerScanJob(Duration.ZERO)) {
                    fail("Expected the operation to reject this request.");
                }
            })
                    .isInstanceOf(ConcurrentLibraryJobException.class).hasCauseInstanceOf(TimeoutException.class);
            assertThatThrownBy(synchronizer::registerDiscoveryTask).isInstanceOf(DiscoveryInterruptedException.class);
        }
    }

    @Test
    void shouldRejectRegistrationOutsideDiscoveryJob() throws Exception {
        assertThatThrownBy(synchronizer::registerDiscoveryTask).isInstanceOf(IllegalStateException.class);
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredScan = synchronizer.registerScanJob(Duration.ZERO)) {
            assertThatThrownBy(synchronizer::registerDiscoveryTask).isInstanceOf(DiscoveryInterruptedException.class);
        }
        LibraryJobSynchronizer.LibraryJobRegistration discovery = synchronizer.registerDiscoveryJob();
        try (LibraryJobSynchronizer.DiscoveryTaskRegistration ignoredTask = synchronizer.registerDiscoveryTask()) {
            discovery.close();
            assertThatThrownBy(synchronizer::registerDiscoveryTask).isInstanceOf(IllegalStateException.class);
            assertBusy();
        }
    }

    @Test
    void shouldRejectNegativeTimeoutWithoutCancelling() throws Exception {
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredDiscovery = synchronizer.registerDiscoveryJob()) {
            assertThatThrownBy(() -> {
                try (LibraryJobSynchronizer.LibraryJobRegistration ignoredUnexpectedRegistration = synchronizer.registerScanJob(Duration.ofSeconds(-1))) {
                    fail("Expected the operation to reject this request.");
                }
            })
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(synchronizer.isDiscoveryCancelled()).isFalse();
        }
    }

    @Test
    void shouldReleaseScanReservationOnInterruptionAndPreserveCancellation() throws Exception {
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredDiscovery = synchronizer.registerDiscoveryJob();
             ExecutorService callers = Executors.newSingleThreadExecutor()) {
            Future<?> waiter = callers.submit(() -> {
                Thread.currentThread().interrupt();
                try {
                    assertThatThrownBy(() -> {
                        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredUnexpectedRegistration = synchronizer.registerScanJob(Duration.ofSeconds(5))) {
                            fail("Expected the operation to reject this request.");
                        }
                    })
                            .isInstanceOf(RuntimeException.class).hasCauseInstanceOf(InterruptedException.class);
                    assertThat(Thread.currentThread().isInterrupted()).isTrue();
                } finally {
                    //noinspection ResultOfMethodCallIgnored
                    Thread.interrupted();
                }
            });
            waiter.get(2, TimeUnit.SECONDS);
            assertThat(synchronizer.isDiscoveryCancelled()).isTrue();
            assertThatThrownBy(synchronizer::registerDiscoveryTask).isInstanceOf(DiscoveryInterruptedException.class);
        }
        try (LibraryJobSynchronizer.LibraryJobRegistration ignoredNext = synchronizer.registerDiscoveryJob()) {
            assertThat(synchronizer.isDiscoveryCancelled()).isFalse();
        }
    }

    private void assertBusy() {
        assertThatThrownBy(synchronizer::registerDiscoveryJob).isInstanceOf(ConcurrentLibraryJobException.class);
    }
}
