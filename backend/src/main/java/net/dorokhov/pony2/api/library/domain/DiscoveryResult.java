package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.UUID;

@Entity
@Table(name = "discovery_result")
public class DiscoveryResult {

    @Id
    @GeneratedValue(strategy = UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "date")
    @NotNull
    private LocalDateTime date;

    @Column(name = "discovery_type")
    @Enumerated(EnumType.STRING)
    @NotNull
    private DiscoveryType type;

    @Column(name = "completed_tasks_count")
    @NotNull
    private Long completedTasks;

    @Column(name = "failed_tasks_count")
    @NotNull
    private Long failedTasks;

    public String getId() {
        return id;
    }

    public DiscoveryResult setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public DiscoveryResult setDate(LocalDateTime date) {
        this.date = date;
        return this;
    }

    public DiscoveryType getType() {
        return type;
    }

    public DiscoveryResult setType(DiscoveryType type) {
        this.type = type;
        return this;
    }

    public Long getCompletedTasks() {
        return completedTasks;
    }

    public DiscoveryResult setCompletedTasks(Long completedTasks) {
        this.completedTasks = completedTasks;
        return this;
    }

    public Long getFailedTasks() {
        return failedTasks;
    }

    public DiscoveryResult setFailedTasks(Long failedTasks) {
        this.failedTasks = failedTasks;
        return this;
    }

    @PrePersist
    public void prePersist() {
        date = LocalDateTime.now();
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }

    @Override
    @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && id != null && getClass().equals(obj.getClass())) {
            DiscoveryResult that = (DiscoveryResult) obj;
            return id.equals(that.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("date", date)
                .add("type", type)
                .add("completedTasks", completedTasks)
                .add("failedTasks", failedTasks)
                .toString();
    }
}
