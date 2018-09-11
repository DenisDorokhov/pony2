package net.dorokhov.pony.test;

import net.dorokhov.pony.api.library.domain.ScanJob;
import net.dorokhov.pony.api.library.domain.ScanType;

import java.time.LocalDateTime;

public final class ScanJobFixtures {
    
    private ScanJobFixtures() {
    }

    public static ScanJob scanJobFull() {
        return scanJob(ScanType.FULL);
    }

    public static ScanJob scanJobEdit() {
        return scanJob(ScanType.EDIT);
    }

    public static ScanJob scanJob(ScanType scanType) {
        return scanJobBuilder(scanType).build();
    }

    public static ScanJob.Builder scanJobBuilder(ScanType scanType) {
        return ScanJob.builder()
                .id("1")
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .status(ScanJob.Status.STARTING)
                .scanType(scanType);
    }
}
