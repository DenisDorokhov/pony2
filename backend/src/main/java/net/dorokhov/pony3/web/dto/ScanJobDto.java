package net.dorokhov.pony3.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony3.api.library.domain.ScanJob;
import net.dorokhov.pony3.api.library.domain.ScanType;

public final class ScanJobDto extends BaseDto<ScanJobDto> {

    private ScanType scanType;
    private ScanJob.Status status;
    private LogMessageDto logMessage;
    private ScanResultDto scanResult;

    public ScanType getScanType() {
        return scanType;
    }

    public ScanJobDto setScanType(ScanType scanType) {
        this.scanType = scanType;
        return this;
    }

    public ScanJob.Status getStatus() {
        return status;
    }

    public ScanJobDto setStatus(ScanJob.Status status) {
        this.status = status;
        return this;
    }

    @Nullable
    public LogMessageDto getLogMessage() {
        return logMessage;
    }

    public ScanJobDto setLogMessage(@Nullable LogMessageDto logMessage) {
        this.logMessage = logMessage;
        return this;
    }

    @Nullable
    public ScanResultDto getScanResult() {
        return scanResult;
    }

    public ScanJobDto setScanResult(@Nullable ScanResultDto scanResult) {
        this.scanResult = scanResult;
        return this;
    }

    public static ScanJobDto of(ScanJob scanJob) {
        return new ScanJobDto()
                .setId(scanJob.getId())
                .setCreationDate(scanJob.getCreationDate())
                .setUpdateDate(scanJob.getUpdateDate())
                .setScanType(scanJob.getScanType())
                .setStatus(scanJob.getStatus())
                .setLogMessage(scanJob.getLogMessage() != null ? LogMessageDto.of(scanJob.getLogMessage()) : null)
                .setScanResult(scanJob.getScanResult() != null ? ScanResultDto.of(scanJob.getScanResult()) : null);
    }
}
