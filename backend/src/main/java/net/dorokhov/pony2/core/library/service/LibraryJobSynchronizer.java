package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.library.service.exception.ConcurrentLibraryJobException;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

/**
 * Coordinates exclusive library jobs and cooperative cancellation of discovery work.
 *
 * <p>Scan and edit requests take priority over discovery: they cancel the current discovery and
 * reserve the next job slot while waiting for its job registration and all task registrations to close.
 * Other job requests are rejected during that wait.
 *
 * <p>All state is protected by one private lock. Job and task registrations may be closed from another
 * thread; they do not hold the lock while application code executes.
 */
@Service
public class LibraryJobSynchronizer {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Set<CancellationSubscription> cancellationSubscriptions = new LinkedHashSet<>();
    private final Object lock = new Object();

    private LibraryJobRegistration activeJob;
    private boolean scanPending;
    private boolean discoveryCancelled;
    private int runningTasks;

    /**
     * Reserves the library for a new discovery job without waiting.
     *
     * <p>A successful registration clears cancellation from the previous discovery. The job registration must
     * cover the whole job lifecycle, including time spent queued, persistence, and cleanup.
     *
     * @return the job registration to close when the job finishes or cannot be started
     * @throws ConcurrentLibraryJobException if another job still holds the library or a scan/edit
     *                                      request is waiting for discovery to finish
     */
    public LibraryJobRegistration registerDiscoveryJob() throws ConcurrentLibraryJobException {
        synchronized (lock) {
            if (activeJob != null || scanPending) {
                throw new ConcurrentLibraryJobException();
            }
            discoveryCancelled = false;
            return activeJob = new LibraryJobRegistration(true);
        }
    }

    /**
     * Reserves the library for a scan or edit job, cancelling and waiting for discovery if needed.
     *
     * <p>New jobs are rejected while this request waits for the discovery job registration and all task registrations
     * to close. Waiting releases the internal lock so discovery can complete. Another scan/edit
     * that already owns or is waiting for the library causes an immediate rejection.
     *
     * <p>Timeout or interruption releases this request's reservation but leaves discovery cancelled.
     * This method must not be called by work whose completion it would need to await.
     *
     * @param timeout the time budget for waiting for discovery; zero allows only immediate registration
     * @return the job registration to close when the scan/edit finishes or cannot be started
     * @throws IllegalArgumentException if the timeout is negative
     * @throws ArithmeticException if the timeout cannot be represented in nanoseconds
     * @throws ConcurrentLibraryJobException if another scan/edit owns or is waiting for the library,
     *                                      or discovery does not finish within the timeout;
     *                                      a timeout is retained as a {@link TimeoutException} cause
     * @throws RuntimeException if the waiting thread is interrupted; the interrupt flag is restored
     *                          and the {@link InterruptedException} is retained as the cause
     */
    public LibraryJobRegistration registerScanJob(Duration timeout) throws ConcurrentLibraryJobException {
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("Cancellation timeout must not be negative.");
        }
        long timeoutNanos = timeout.toNanos();
        long started = System.nanoTime();
        List<CancellationSubscription> subscriptions;
        synchronized (lock) {
            if (scanPending || (activeJob != null && !activeJob.discovery)) {
                throw new ConcurrentLibraryJobException();
            }
            scanPending = true;
            subscriptions = cancelDiscoveryLocked();
        }
        try {
            notifyCancellation(subscriptions);
            synchronized (lock) {
                // Wait for the discovery job and all registered tasks to close. timedWait releases
                // lock so they can finish and notify us. scanPending reserves the next job slot,
                // including the gap between activeJob being cleared and this thread reacquiring
                // lock to register the scan/edit job.
                while (activeJob != null) {
                    long remaining = timeoutNanos - (System.nanoTime() - started);
                    if (remaining <= 0) {
                        throw new ConcurrentLibraryJobException(
                                "Timed out waiting for discovery to finish.",
                                new TimeoutException("Discovery did not finish cancellation within " + timeout + "."));
                    }
                    TimeUnit.NANOSECONDS.timedWait(lock, remaining);
                }
                return activeJob = new LibraryJobRegistration(false);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for discovery cancellation.", e);
        } finally {
            synchronized (lock) {
                scanPending = false;
            }
        }
    }

    /**
     * Registers a task belonging to the current discovery job, including nested or queued work.
     *
     * <p>Registration checks cancellation atomically and never clears it. For asynchronous work,
     * register before submission and close the registration after execution, persistence, and cleanup;
     * also close it if submission fails or the task will never execute.
     *
     * @return the task registration, suitable for try-with-resources during synchronous execution
     * @throws DiscoveryInterruptedException if discovery cancellation has been requested
     * @throws IllegalStateException if no discovery job is accepting tasks, including when its
     *                               job registration has already been closed
     */
    public DiscoveryTaskRegistration registerDiscoveryTask() {
        synchronized (lock) {
            if (discoveryCancelled) {
                throw new DiscoveryInterruptedException();
            }
            if (activeJob == null || !activeJob.discovery || activeJob.closed) {
                throw new IllegalStateException("No discovery job is accepting tasks.");
            }
            runningTasks++;
            return new DiscoveryTaskRegistration();
        }
    }

    /**
     * Requests cooperative discovery cancellation without waiting or interrupting worker threads.
     *
     * <p>New task registrations and subsequent cancellation checks fail until a new discovery
     * job is registered. Cancellation remains set even if no discovery is currently active.
     */
    public void cancelDiscovery() {
        List<CancellationSubscription> subscriptions;
        synchronized (lock) {
            subscriptions = cancelDiscoveryLocked();
        }
        notifyCancellation(subscriptions);
    }

    /**
     * Subscribes to the current discovery's cancellation. An already cancelled discovery invokes the callback immediately.
     * Callbacks run outside the internal lock and must return promptly. Closing the subscription unregisters it;
     * a callback already selected for notification may still run after closing.
     */
    public CancellationSubscription onCancel(Runnable callback) {
        CancellationSubscription subscription = new CancellationSubscription(requireNonNull(callback));
        synchronized (lock) {
            if (!discoveryCancelled) {
                if (activeJob == null || !activeJob.discovery) {
                    throw new IllegalStateException("No discovery job is active.");
                }
                cancellationSubscriptions.add(subscription);
                return subscription;
            }
        }
        notifyCancellation(List.of(subscription));
        return subscription;
    }

    private List<CancellationSubscription> cancelDiscoveryLocked() {
        discoveryCancelled = true;
        List<CancellationSubscription> subscriptions = new ArrayList<>(cancellationSubscriptions);
        cancellationSubscriptions.clear();
        return subscriptions;
    }

    private void notifyCancellation(List<CancellationSubscription> subscriptions) {
        for (CancellationSubscription subscription : subscriptions) {
            try {
                subscription.callback.run();
            } catch (RuntimeException e) {
                logger.warn("Could not cancel discovery operation.", e);
            }
        }
    }

    /**
     * Checks for cancellation at a cooperative stopping point in discovery code.
     * This check does not change the calling thread's interrupt flag.
     *
     * @throws DiscoveryInterruptedException if discovery cancellation has been requested
     */
    public void interruptDiscoveryIfCancelled() {
        synchronized (lock) {
            if (discoveryCancelled) {
                throw new DiscoveryInterruptedException();
            }
        }
    }

    /**
     * Returns the current discovery cancellation flag, which may remain set after discovery ends.
     *
     * @return {@code true} if cancellation has been requested and no new discovery job registered
     */
    public boolean isDiscoveryCancelled() {
        synchronized (lock) {
            return discoveryCancelled;
        }
    }

    /**
     * Reports whether any discovery task registrations remain open, including registrations for queued work.
     * A job registration may still be open even when this method returns {@code false}.
     *
     * @return {@code true} if at least one discovery task is registered
     */
    public boolean hasRunningTasks() {
        synchronized (lock) {
            return runningTasks > 0;
        }
    }

    /**
     * Releases the active job and wakes the waiting scan once its job registration and all task registrations close.
     * The caller must hold {@link #lock} and have an active job.
     */
    private void releaseIfFinished() {
        if (activeJob.closed && runningTasks == 0) {
            activeJob = null;
            cancellationSubscriptions.clear();
            lock.notifyAll();
        }
    }

    public class CancellationSubscription implements AutoCloseable {

        private final Runnable callback;

        private CancellationSubscription(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void close() {
            synchronized (lock) {
                cancellationSubscriptions.remove(this);
            }
        }
    }

    /**
     * Registration granting exclusive access to the library for a discovery, scan, or edit job.
     *
     * <p>The registration spans the whole job lifecycle and may be passed to an executor thread.
     * Closing it marks the job finished; outstanding discovery task registrations continue to reserve
     * the library until they also close.
     */
    public class LibraryJobRegistration implements AutoCloseable {

        private final boolean discovery;
        private boolean closed;

        private LibraryJobRegistration(boolean discovery) {
            this.discovery = discovery;
        }

        /**
         * Marks the job finished and releases the library once all its task registrations have closed.
         * For discovery, this also prevents further task registrations.
         *
         * <p>May be called from any thread. Repeated calls have no effect.
         */
        @Override
        public void close() {
            synchronized (lock) {
                if (!closed) {
                    closed = true;
                    releaseIfFinished();
                }
            }
        }
    }

    /**
     * Registration of one discovery task, keeping the library reserved until that task completes.
     * Registrations may overlap or be nested; each successful registration must be closed separately.
     */
    public class DiscoveryTaskRegistration implements AutoCloseable {

        private boolean closed;

        private DiscoveryTaskRegistration() {
        }

        /**
         * Completes this task registration after execution, persistence, and cleanup.
         * Releases the library if the job registration and all other registrations have also closed.
         *
         * <p>May be called from any thread. Repeated calls have no effect and do not clear cancellation.
         */
        @Override
        public void close() {
            synchronized (lock) {
                if (!closed) {
                    closed = true;
                    runningTasks--;
                    releaseIfFinished();
                }
            }
        }
    }
}
