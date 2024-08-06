package net.dorokhov.pony2.test;

import net.dorokhov.pony2.api.library.domain.ScanJob;
import net.dorokhov.pony2.api.library.domain.ScanType;

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
        return new ScanJob()
                .setId("1")
                .setCreationDate(LocalDateTime.now())
                .setUpdateDate(LocalDateTime.now())
                .setStatus(ScanJob.Status.STARTING)
                .setScanType(scanType);
    }
}
