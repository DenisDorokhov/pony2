package net.dorokhov.pony.web.domain;

import net.dorokhov.pony.api.library.domain.ScanJob;
import net.dorokhov.pony.api.library.domain.ScanType;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ScanJobDto extends BaseDto {

    private final ScanType scanType;
    private final ScanJob.Status status;
    private final LogMessageDto logMessage;
    private final ScanResultDto scanResult;

    private ScanJobDto(String id, LocalDateTime creationDate, @Nullable LocalDateTime updateDate,
                       ScanType scanType, ScanJob.Status status, @Nullable LogMessageDto logMessage,
                       @Nullable ScanResultDto scanResult) {
        super(id, creationDate, updateDate);
        this.scanType = checkNotNull(scanType);
        this.status = checkNotNull(status);
        this.logMessage = logMessage;
        this.scanResult = scanResult;
    }

    public ScanType getScanType() {
        return scanType;
    }

    public ScanJob.Status getStatus() {
        return status;
    }

    @Nullable
    public LogMessageDto getLogMessage() {
        return logMessage;
    }

    @Nullable
    public ScanResultDto getScanResult() {
        return scanResult;
    }

    public static ScanJobDto of(ScanJob scanJob) {
        return new ScanJobDto(scanJob.getId(), scanJob.getCreationDate(), scanJob.getUpdateDate(),
                scanJob.getScanType(), scanJob.getStatus(),
                scanJob.getLogMessage() != null ? LogMessageDto.of(scanJob.getLogMessage()) : null,
                scanJob.getScanResult() != null ? ScanResultDto.of(scanJob.getScanResult()) : null);
    }
}
