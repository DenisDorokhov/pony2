package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.api.common.BaseEntity;

@Entity
@Table(name = "discovery_task")
public class DiscoveryTask extends BaseEntity<DiscoveryTask> {

    public enum Status {
        STARTED, COMPLETE, FAILED, INTERRUPTED
    }

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @NotNull
    private Status status;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @NotNull
    private DiscoveryTaskType type;

    @Column(name = "argument")
    @NotNull
    private String argument;

    @Column(name = "result")
    private String result;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "discovery_job_id", nullable = false)
    @NotNull
    private DiscoveryJob job;

    public Status getStatus() {
        return status;
    }

    public DiscoveryTask setStatus(Status status) {
        this.status = status;
        return this;
    }

    public DiscoveryTaskType getType() {
        return type;
    }

    public DiscoveryTask setType(DiscoveryTaskType type) {
        this.type = type;
        return this;
    }

    public String getArgument() {
        return argument;
    }

    public DiscoveryTask setArgument(String argument) {
        this.argument = argument;
        return this;
    }

    @Nullable
    public String getResult() {
        return result;
    }

    public DiscoveryTask setResult(@Nullable String result) {
        this.result = result;
        return this;
    }

    public DiscoveryJob getJob() {
        return job;
    }

    public DiscoveryTask setJob(DiscoveryJob job) {
        this.job = job;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("updateDate", updateDate)
                .add("status", status)
                .add("type", type)
                .add("argument", argument)
                .add("result", result)
                .toString();
    }
}
