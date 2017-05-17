package net.dorokhov.pony.library.domain;

public final class ScanJobProgress {
    
    private final ScanJob scanJob;
    private final ScanStatus.Progress scanStatus;

    public ScanJobProgress(ScanJob scanJob, ScanStatus.Progress scanStatus) {
        this.scanJob = scanJob;
        this.scanStatus = scanStatus;
    }

    public ScanJob getScanJob() {
        return scanJob;
    }

    public ScanStatus.Progress getScanStatus() {
        return scanStatus;
    }
}
