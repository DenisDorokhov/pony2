package net.dorokhov.pony2;

import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import net.dorokhov.pony2.api.library.domain.DiscoveryJobProgress;
import net.dorokhov.pony2.api.library.service.DiscoveryJobService;

import java.util.concurrent.CountDownLatch;

/**
 * Blocks discovery until unlocked to check discovery progress calls.
 */
public final class BlockingDiscoveryJobServiceObserver implements DiscoveryJobService.Observer {

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    @Override
    public void onDiscoveryJobStarting(DiscoveryJob discoveryJob) {
    }

    @Override
    public void onDiscoveryJobStarted(DiscoveryJob discoveryJob) {
    }

    @Override
    public void onDiscoveryJobProgress(DiscoveryJobProgress discoveryJobProgress) {
    }

    @Override
    public void onDiscoveryJobCompleting(DiscoveryJob discoveryJob) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDiscoveryJobCompleted(DiscoveryJob discoveryJob) {
    }

    @Override
    public void onDiscoveryJobModerating(DiscoveryJob discoveryJob) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDiscoveryJobModerated(DiscoveryJob discoveryJob) {
    }

    @Override
    public void onDiscoveryJobFailing(DiscoveryJob discoveryJob) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDiscoveryJobFailed(DiscoveryJob discoveryJob) {
    }

    @Override
    public void onDiscoveryJobInterrupting(DiscoveryJob discoveryJob) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDiscoveryJobInterrupted(DiscoveryJob discoveryJob) {
    }

    public void unlock() {
        countDownLatch.countDown();
    }
}
