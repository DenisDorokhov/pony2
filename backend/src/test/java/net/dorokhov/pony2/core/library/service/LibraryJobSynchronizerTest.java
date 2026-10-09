package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

class LibraryJobSynchronizerTest {

    private final LibraryJobSynchronizer synchronizer = new LibraryJobSynchronizer();

    @Test
    void shouldReleaseFromAnotherThreadOnlyOnce() throws Exception {
        LibraryJobSynchronizer.LibraryJobRegistration first = synchronizer.registerScanJob(Duration.ZERO);
        assertBusy();
        Thread worker = new Thread(first::close);
        worker.start();
        worker.join(5000);
        assertThat(worker.isAlive()).isFalse();

        try (LibraryJobSynchronizer.LibraryJobRegistration second = synchronizer.registerDiscoveryJob()) {
            first.close();
            assertBusy();
        }
        try (LibraryJobSynchronizer.LibraryJobRegistration third = synchronizer.registerScanJob(Duration.ZERO)) {
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
                assertThatThrownBy(() -> synchronizer.registerScanJob(Duration.ZERO))
                        .isInstanceOf(ConcurrentLibraryJobException.class);

                discovery.close();
                firstTask.close();
                firstTask.close();
                assertThat(scan.isDone()).isFalse();
                assertBusy();
                secondTask.close();

                try (LibraryJobSynchronizer.LibraryJobRegistration jobRegistration = scan.get(2, TimeUnit.SECONDS)) {
                    assertThat(synchronizer.hasRunningTasks()).isFalse();
                    assertBusy();
                    discovery.close();
                    secondTask.close();
                    assertBusy();
                }
                try (LibraryJobSynchronizer.LibraryJobRegistration next = synchronizer.registerDiscoveryJob()) {
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
        try (LibraryJobSynchronizer.LibraryJobRegistration discovery = synchronizer.registerDiscoveryJob()) {
            try (LibraryJobSynchronizer.DiscoveryTaskRegistration task = synchronizer.registerDiscoveryTask()) {
                assertThat(synchronizer.hasRunningTasks()).isTrue();
            }
            assertThat(synchronizer.hasRunningTasks()).isFalse();
            assertBusy();
            try (LibraryJobSynchronizer.DiscoveryTaskRegistration nextTask = synchronizer.registerDiscoveryTask()) {
                assertThat(synchronizer.hasRunningTasks()).isTrue();
            }
        }
    }

    @Test
    void shouldKeepCancellationAfterTimeoutUntilNextDiscoveryJob() throws Exception {
        try (LibraryJobSynchronizer.LibraryJobRegistration discovery = synchronizer.registerDiscoveryJob()) {
            try (LibraryJobSynchronizer.DiscoveryTaskRegistration task = synchronizer.registerDiscoveryTask()) {
                assertThatThrownBy(() -> synchronizer.registerScanJob(Duration.ofMillis(20)))
                        .isInstanceOf(ConcurrentLibraryJobException.class)
                        .hasMessage("Timed out waiting for discovery to finish.")
                        .hasCauseInstanceOf(TimeoutException.class);
                assertThatThrownBy(synchronizer::interruptDiscoveryIfCancelled).isInstanceOf(DiscoveryInterruptedException.class);
            }
            // Finishing the last task must not let another task reset this job's cancellation.
            assertThatThrownBy(synchronizer::registerDiscoveryTask).isInstanceOf(DiscoveryInterruptedException.class);
        }
        try (LibraryJobSynchronizer.LibraryJobRegistration next = synchronizer.registerDiscoveryJob();
             LibraryJobSynchronizer.DiscoveryTaskRegistration task = synchronizer.registerDiscoveryTask()) {
            assertThat(synchronizer.isDiscoveryCancelled()).isFalse();
        }
        try (LibraryJobSynchronizer.LibraryJobRegistration scan = synchronizer.registerScanJob(Duration.ZERO)) {
            assertBusy();
        }
    }

    @Test
    void shouldCancelQueuedDiscoveryEvenWithoutRegisteredTasks() throws Exception {
        try (LibraryJobSynchronizer.LibraryJobRegistration discovery = synchronizer.registerDiscoveryJob()) {
            assertThatThrownBy(() -> synchronizer.registerScanJob(Duration.ZERO))
                    .isInstanceOf(ConcurrentLibraryJobException.class).hasCauseInstanceOf(TimeoutException.class);
            assertThatThrownBy(synchronizer::registerDiscoveryTask).isInstanceOf(DiscoveryInterruptedException.class);
        }
    }

    @Test
    void shouldRejectRegistrationOutsideDiscoveryJob() throws Exception {
        assertThatThrownBy(synchronizer::registerDiscoveryTask).isInstanceOf(IllegalStateException.class);
        try (LibraryJobSynchronizer.LibraryJobRegistration scan = synchronizer.registerScanJob(Duration.ZERO)) {
            assertThatThrownBy(synchronizer::registerDiscoveryTask).isInstanceOf(DiscoveryInterruptedException.class);
        }
        LibraryJobSynchronizer.LibraryJobRegistration discovery = synchronizer.registerDiscoveryJob();
        try (LibraryJobSynchronizer.DiscoveryTaskRegistration task = synchronizer.registerDiscoveryTask()) {
            discovery.close();
            assertThatThrownBy(synchronizer::registerDiscoveryTask).isInstanceOf(IllegalStateException.class);
            assertBusy();
        }
    }

    @Test
    void shouldRejectNegativeTimeoutWithoutCancelling() throws Exception {
        try (LibraryJobSynchronizer.LibraryJobRegistration discovery = synchronizer.registerDiscoveryJob()) {
            assertThatThrownBy(() -> synchronizer.registerScanJob(Duration.ofSeconds(-1)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(synchronizer.isDiscoveryCancelled()).isFalse();
        }
    }

    @Test
    void shouldReleaseScanReservationOnInterruptionAndPreserveCancellation() throws Exception {
        try (LibraryJobSynchronizer.LibraryJobRegistration discovery = synchronizer.registerDiscoveryJob();
             ExecutorService callers = Executors.newSingleThreadExecutor()) {
            Future<?> waiter = callers.submit(() -> {
                Thread.currentThread().interrupt();
                try {
                    assertThatThrownBy(() -> synchronizer.registerScanJob(Duration.ofSeconds(5)))
                            .isInstanceOf(RuntimeException.class).hasCauseInstanceOf(InterruptedException.class);
                    assertThat(Thread.currentThread().isInterrupted()).isTrue();
                } finally {
                    Thread.interrupted();
                }
            });
            waiter.get(2, TimeUnit.SECONDS);
            assertThat(synchronizer.isDiscoveryCancelled()).isTrue();
            assertThatThrownBy(synchronizer::registerDiscoveryTask).isInstanceOf(DiscoveryInterruptedException.class);
        }
        try (LibraryJobSynchronizer.LibraryJobRegistration next = synchronizer.registerDiscoveryJob()) {
            assertThat(synchronizer.isDiscoveryCancelled()).isFalse();
        }
    }

    private void assertBusy() {
        assertThatThrownBy(synchronizer::registerDiscoveryJob).isInstanceOf(ConcurrentLibraryJobException.class);
    }
}
