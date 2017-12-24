package net.dorokhov.pony.api.library.domain;

import javax.annotation.Nullable;

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
        return "ScanJobProgress{" +
                "scanJob=" + scanJob +
                ", scanProgress=" + scanProgress +
                '}';
    }
}
