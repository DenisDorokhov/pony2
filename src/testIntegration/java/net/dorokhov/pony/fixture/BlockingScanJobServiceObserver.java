package net.dorokhov.pony.fixture;

import net.dorokhov.pony.library.domain.ScanJob;
import net.dorokhov.pony.library.domain.ScanJobProgress;
import net.dorokhov.pony.library.service.ScanJobService;

import java.util.concurrent.CountDownLatch;

/**
 * Blocks scan until unlocked to check scan progress calls.
 */
public final class BlockingScanJobServiceObserver implements ScanJobService.Observer {
    
    private CountDownLatch countDownLatch  = new CountDownLatch(1);

    @Override
    public void onScanJobStarting(ScanJob scanJob) {
    }

    @Override
    public void onScanJobStarted(ScanJob scanJob) {
    }

    @Override
    public void onScanJobProgress(ScanJobProgress scanJobProgress) {
    }

    @Override
    public void onScanJobCompleting(ScanJob scanJob) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onScanJobCompleted(ScanJob scanJob) {
    }

    @Override
    public void onScanJobFailing(ScanJob scanJob) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onScanJobFailed(ScanJob scanJob) {
    }
    
    public void unlock() {
        countDownLatch.countDown();
    }
}
