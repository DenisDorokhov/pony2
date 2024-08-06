package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.ScanJobProgress;

public final class ScanJobProgressDto {

    private ScanJobDto scanJob;
    private ScanProgressDto scanProgress;

    public ScanJobDto getScanJob() {
        return scanJob;
    }

    public ScanJobProgressDto setScanJob(ScanJobDto scanJob) {
        this.scanJob = scanJob;
        return this;
    }

    @Nullable
    public ScanProgressDto getScanProgress() {
        return scanProgress;
    }

    public ScanJobProgressDto setScanProgress(ScanProgressDto scanProgress) {
        this.scanProgress = scanProgress;
        return this;
    }

    public static ScanJobProgressDto of(ScanJobProgress scanJobProgress) {
        return new ScanJobProgressDto()
                .setScanJob(ScanJobDto.of(scanJobProgress.getScanJob()))
                .setScanProgress(scanJobProgress.getScanProgress() != null ? ScanProgressDto.of(scanJobProgress.getScanProgress()) : null);
    }
}
