package net.dorokhov.pony3.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.annotation.Nullable;

public final class ScanJobProgress {
    
    private final ScanJob scanJob;
    private final ScanProgress scanProgress;

    public ScanJobProgress(ScanJob scanJob, @Nullable ScanProgress scanProgress) {
        this.scanJob = scanJob;
        this.scanProgress = scanProgress;
    }

    public ScanJob getScanJob() {
        return scanJob;
    }

    @Nullable
    public ScanProgress getScanProgress() {
        return scanProgress;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("scanJob", scanJob)
                .add("scanProgress", scanProgress)
                .toString();
    }
}
