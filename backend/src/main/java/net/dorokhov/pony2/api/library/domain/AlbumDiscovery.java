package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.api.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "album_discovery")
public class AlbumDiscovery extends BaseEntity<AlbumDiscovery> {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    @NotNull
    private Album album;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "discovery_job_id", nullable = false)
    @NotNull
    private DiscoveryJob job;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "album_discovery_task",
            joinColumns = @JoinColumn(name = "album_discovery_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "discovery_task_id", nullable = false)
    )
    private List<DiscoveryTask> tasks = new ArrayList<>();

    public Album getAlbum() {
        return album;
    }

    public AlbumDiscovery setAlbum(Album album) {
        this.album = album;
        return this;
    }

    public DiscoveryJob getJob() {
        return job;
    }

    public AlbumDiscovery setJob(DiscoveryJob job) {
        this.job = job;
        return this;
    }

    public List<DiscoveryTask> getTasks() {
        return tasks;
    }

    public AlbumDiscovery setTasks(List<DiscoveryTask> tasks) {
        this.tasks = tasks;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("updateDate", updateDate)
                .add("album", album != null ? album.getId() : "null")
                .add("job", job != null ? job.getId() : "null")
                .toString();
    }
}
