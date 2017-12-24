package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.ScanJobProgress;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ScanJobProgressDto {

    private final ScanJobDto scanJob;
    private final ScanProgressDto scanProgress;

    ScanJobProgressDto(ScanJobDto scanJob, ScanProgressDto scanProgress) {
        this.scanJob = checkNotNull(scanJob);
        this.scanProgress = scanProgress;
    }

    public ScanJobDto getScanJob() {
        return scanJob;
    }

    public ScanProgressDto getScanProgress() {
        return scanProgress;
    }

    public static ScanJobProgressDto of(ScanJobProgress scanJobProgress) {
        return new ScanJobProgressDto(ScanJobDto.of(scanJobProgress.getScanJob()),
                scanJobProgress.getScanProgress() != null ? ScanProgressDto.of(scanJobProgress.getScanProgress()) : null);
    }
}
