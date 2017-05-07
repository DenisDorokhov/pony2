package net.dorokhov.pony.fixture;

import net.dorokhov.pony.library.domain.ScanJob;
import net.dorokhov.pony.library.domain.ScanType;

import java.time.LocalDateTime;

public final class ScanJobFixtures {

    private ScanJobFixtures() {
    }

    public static ScanJob full() {
        return get(ScanType.FULL);
    }

    public static ScanJob edit() {
        return get(ScanType.EDIT);
    }

    public static ScanJob get(ScanType scanType) {
        return builder(scanType).build();
    }
    
    public static ScanJob.Builder builder(ScanType scanType) {
        return ScanJob.builder()
                .id(1L)
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .status(ScanJob.Status.STARTING)
                .scanType(scanType);
    }
}
