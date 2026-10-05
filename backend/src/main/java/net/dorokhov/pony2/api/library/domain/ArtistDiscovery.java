package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.api.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "artist_discovery")
public class ArtistDiscovery extends BaseEntity<ArtistDiscovery> {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    @NotNull
    private Artist artist;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "discovery_job_id", nullable = false)
    @NotNull
    private DiscoveryJob job;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "artist_discovery_task",
            joinColumns = @JoinColumn(name = "artist_discovery_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "discovery_task_id", nullable = false)
    )
    private List<DiscoveryTask> tasks = new ArrayList<>();

    public Artist getArtist() {
        return artist;
    }

    public ArtistDiscovery setArtist(Artist artist) {
        this.artist = artist;
        return this;
    }

    public DiscoveryJob getJob() {
        return job;
    }

    public ArtistDiscovery setJob(DiscoveryJob job) {
        this.job = job;
        return this;
    }

    public List<DiscoveryTask> getTasks() {
        return tasks;
    }

    public ArtistDiscovery setTasks(List<DiscoveryTask> tasks) {
        this.tasks = tasks;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("updateDate", updateDate)
                .add("artist", artist != null ? artist.getId() : "null")
                .add("job", job != null ? job.getId() : "null")
                .toString();
    }
}
