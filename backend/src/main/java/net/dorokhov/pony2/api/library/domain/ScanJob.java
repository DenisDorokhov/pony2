package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.api.common.BaseEntity;
import net.dorokhov.pony2.api.log.domain.LogMessage;
import net.dorokhov.pony2.common.ListOfStringsJsonAttributeConverter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scan_job")
public class ScanJob extends BaseEntity<ScanJob> {

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

    @Column(name = "target_paths")
    @Convert(converter = ListOfStringsJsonAttributeConverter.class)
    @NotNull
    private List<String> targetPaths = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_message_id", unique = true)
    private LogMessage logMessage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_result_id", unique = true)
    private ScanResult scanResult;

    public ScanType getScanType() {
        return scanType;
    }

    public ScanJob setScanType(ScanType scanType) {
        this.scanType = scanType;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public ScanJob setStatus(Status status) {
        this.status = status;
        return this;
    }

    public List<String> getTargetPaths() {
        if (targetPaths == null) {
            targetPaths = new ArrayList<>();
        }
        return targetPaths;
    }

    public ScanJob setTargetPaths(List<String> targetPaths) {
        this.targetPaths = targetPaths;
        return this;
    }

    @Nullable
    public LogMessage getLogMessage() {
        return logMessage;
    }

    public ScanJob setLogMessage(@Nullable LogMessage logMessage) {
        this.logMessage = logMessage;
        return this;
    }

    @Nullable
    public ScanResult getScanResult() {
        return scanResult;
    }

    public ScanJob setScanResult(@Nullable ScanResult scanResult) {
        this.scanResult = scanResult;
        return this;
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
}
