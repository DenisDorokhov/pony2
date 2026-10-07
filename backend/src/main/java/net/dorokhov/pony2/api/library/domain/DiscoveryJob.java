package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.api.common.BaseEntity;
import net.dorokhov.pony2.api.log.domain.LogMessage;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discovery_job")
public class DiscoveryJob extends BaseEntity<DiscoveryJob> {

    public enum Status {
        STARTING, STARTED, COMPLETE, MODERATE, FAILED, INTERRUPTED
    }

    @Column(name = "discovery_type")
    @Enumerated(EnumType.STRING)
    @NotNull
    private DiscoveryType type;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @NotNull
    private Status status;

    @Column(name = "parameter")
    private String parameter;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_message_id", unique = true)
    private LogMessage logMessage;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "job")
    private List<DiscoveryTask> tasks = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "discovery_result_id", unique = true)
    private DiscoveryResult discoveryResult;

    public DiscoveryType getType() {
        return type;
    }

    public DiscoveryJob setType(DiscoveryType type) {
        this.type = type;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public DiscoveryJob setStatus(Status status) {
        this.status = status;
        return this;
    }

    public String getParameter() {
        return parameter;
    }

    public DiscoveryJob setParameter(String parameter) {
        this.parameter = parameter;
        return this;
    }

    public LogMessage getLogMessage() {
        return logMessage;
    }

    public DiscoveryJob setLogMessage(LogMessage logMessage) {
        this.logMessage = logMessage;
        return this;
    }

    public List<DiscoveryTask> getTasks() {
        return tasks;
    }

    public DiscoveryJob setTasks(List<DiscoveryTask> tasks) {
        this.tasks = tasks;
        return this;
    }

    public DiscoveryResult getDiscoveryResult() {
        return discoveryResult;
    }

    public DiscoveryJob setDiscoveryResult(DiscoveryResult discoveryResult) {
        this.discoveryResult = discoveryResult;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("updateDate", updateDate)
                .add("type", type)
                .add("status", status)
                .add("parameter", parameter)
                .toString();
    }
}
