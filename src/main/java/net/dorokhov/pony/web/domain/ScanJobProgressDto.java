package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.library.domain.ScanJobProgress;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ScanJobProgressDto {

    private final ScanJobDto scanJob;
    private final ScanProgressDto scanProgressDto;

    ScanJobProgressDto(ScanJobDto scanJob, ScanProgressDto scanProgressDto) {
        this.scanJob = checkNotNull(scanJob);
        this.scanProgressDto = checkNotNull(scanProgressDto);
    }

    public ScanJobDto getScanJob() {
        return scanJob;
    }

    public ScanProgressDto getScanProgressDto() {
        return scanProgressDto;
    }

    public static ScanJobProgressDto of(ScanJobProgress scanJobProgress) {
        return new ScanJobProgressDto(ScanJobDto.of(scanJobProgress.getScanJob()), ScanProgressDto.of(scanJobProgress.getScanProgress()));
    }
}
