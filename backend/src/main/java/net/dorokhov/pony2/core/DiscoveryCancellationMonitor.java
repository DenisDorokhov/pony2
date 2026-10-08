package net.dorokhov.pony2.core;

import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Coordinates cooperative cancellation of discovery tasks before a scan.
 */
@Component
public final class DiscoveryCancellationMonitor {

    private int runningTasks;
    private volatile boolean cancelled;
    private long generation;

    /**
     * Atomically rejects cancellation or registers work, including work queued in an executor.
     */
    public synchronized void taskStarted() {
        // A new group starts only after all work from the cancelled group has finished.
        if (cancelled && runningTasks == 0) {
            cancelled = false;
            generation++;
        }
        interruptIfCancelled();
        runningTasks++;
    }

    /**
     * Completes one successful registration after persistence and cleanup, without clearing cancellation.
     */
    public synchronized void taskFinished() {
        if (runningTasks == 0) {
            throw new IllegalStateException("No discovery task is registered.");
        }
        if (--runningTasks == 0) {
            notifyAll();
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Permanently cancels this group, even when it is idle, without waiting.
     */
    public synchronized void cancel() {
        cancelled = true;
    }

    /**
     * Cancels and waits within a total timeout. Timeout and interruption do not clear cancellation.
     * Interruption preserves the thread interrupt flag and is wrapped in a RuntimeException.
     * Must not be called from a task registered with this monitor.
     */
    public synchronized void cancelAndWait(Duration timeout) throws TimeoutException {
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("Cancellation timeout must not be negative.");
        }
        long timeoutNanos = timeout.toNanos();
        long started = System.nanoTime();
        cancel();
        // A delayed waiter must not start waiting for tasks in a later group.
        long cancelledGeneration = generation;
        while (generation == cancelledGeneration && runningTasks > 0) {
            long remaining = timeoutNanos - (System.nanoTime() - started);
            if (remaining <= 0) {
                throw new TimeoutException("Discovery tasks did not finish cancellation within " + timeout + ".");
            }
            try {
                TimeUnit.NANOSECONDS.timedWait(this, remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for discovery cancellation.", e);
            }
        }
    }

    /**
     * Called by task implementations at their cooperative cancellation points.
     */
    public void interruptIfCancelled() {
        if (cancelled) {
            throw new DiscoveryInterruptedException();
        }
    }

    public synchronized boolean hasRunningTasks() {
        return runningTasks > 0;
    }
}
