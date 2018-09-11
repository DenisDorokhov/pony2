package net.dorokhov.pony.api.library.domain;

import com.google.common.base.MoreObjects;
import net.dorokhov.pony.api.common.BaseEntity;
import net.dorokhov.pony.api.log.domain.LogMessage;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "scan_job")
public class ScanJob extends BaseEntity {

    public enum Status {
        STARTING, STARTED, COMPLETE, FAILED, INTERRUPTED
    }

    @Column(name = "scan_type")
    @Enumerated(EnumType.STRING)
    @NotNull
    private ScanType scanType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @NotNull
    private Status status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_message_id", unique = true)
    private LogMessage logMessage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_result_id", unique = true)
    private ScanResult scanResult;

    protected ScanJob() {
    }

    private ScanJob(Builder builder) {
        id = builder.id;
        creationDate = builder.creationDate;
        updateDate = builder.updateDate;
        scanType = checkNotNull(builder.scanType);
        status = checkNotNull(builder.status);
        logMessage = builder.logMessage;
        scanResult = builder.scanResult;
    }

    public ScanType getScanType() {
        return scanType;
    }

    public Status getStatus() {
        return status;
    }

    @Nullable
    public LogMessage getLogMessage() {
        return logMessage;
    }

    @Nullable
    public ScanResult getScanResult() {
        return scanResult;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("updateDate", updateDate)
                .add("scanType", scanType)
                .add("status", status)
                .add("logMessage", logMessage)
                .add("scanResult", scanResult)
                .toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ScanJob scanJob) {
        return new Builder(scanJob);
    }

    public static final class Builder {
        
        private String id;
        private ScanType scanType;
        private Status status;
        private LocalDateTime creationDate;
        private LocalDateTime updateDate;
        private LogMessage logMessage;
        private ScanResult scanResult;

        private Builder() {
        }

        private Builder(ScanJob scanJob) {
            this.id = scanJob.getId();
            this.scanType = scanJob.getScanType();
            this.status = scanJob.getStatus();
            this.creationDate = scanJob.getCreationDate();
            this.updateDate = scanJob.getUpdateDate();
            this.logMessage = scanJob.getLogMessage();
            this.scanResult = scanJob.getScanResult();
        }

        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        public Builder creationDate(@Nullable LocalDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder updateDate(@Nullable LocalDateTime updateDate) {
            this.updateDate = updateDate;
            return this;
        }

        public Builder scanType(ScanType type) {
            this.scanType = type;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder logMessage(@Nullable LogMessage logMessage) {
            this.logMessage = logMessage;
            return this;
        }

        public Builder scanResult(@Nullable ScanResult scanResult) {
            this.scanResult = scanResult;
            return this;
        }

        public ScanJob build() {
            return new ScanJob(this);
        }
    }
}
