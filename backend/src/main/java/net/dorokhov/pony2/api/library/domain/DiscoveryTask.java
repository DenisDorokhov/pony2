package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.api.common.BaseEntity;
import net.dorokhov.pony2.common.ListOfObjectsJsonAttributeConverter;

import java.util.List;

@Entity
@Table(name = "discovery_task")
public class DiscoveryTask extends BaseEntity<DiscoveryTask> {

    public enum Status {
        STARTED, COMPLETE, FAILED, INTERRUPTED
    }

    public record ErrorResult(String error) {}

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @NotNull
    private Status status;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @NotNull
    private DiscoveryTaskType type;

    @Column(name = "result")
    private String result;

    @Column(name = "raw_requests")
    @Convert(converter = ListOfObjectsJsonAttributeConverter.class)
    private List<Object> rawRequests;

    @Column(name = "raw_responses")
    @Convert(converter = ListOfObjectsJsonAttributeConverter.class)
    private List<Object> rawResponses;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "discovery_job_id", nullable = false)
    @NotNull
    private DiscoveryJob job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id")
    private Genre genre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    private Song song;

    public @Nullable Genre getGenre() {
        return genre;
    }

    public DiscoveryTask setGenre(@Nullable Genre genre) {
        this.genre = genre;
        return this;
    }

    public @Nullable Artist getArtist() {
        return artist;
    }

    public DiscoveryTask setArtist(@Nullable Artist artist) {
        this.artist = artist;
        return this;
    }

    public @Nullable Album getAlbum() {
        return album;
    }

    public DiscoveryTask setAlbum(@Nullable Album album) {
        this.album = album;
        return this;
    }

    public @Nullable Song getSong() {
        return song;
    }

    public DiscoveryTask setSong(@Nullable Song song) {
        this.song = song;
        return this;
    }

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

    public @Nullable String getResult() {
        return result;
    }

    public DiscoveryTask setResult(@Nullable String result) {
        this.result = result;
        return this;
    }

    public @Nullable List<Object> getRawRequests() {
        return rawRequests;
    }

    public DiscoveryTask setRawRequests(@Nullable List<Object> rawRequests) {
        this.rawRequests = rawRequests;
        return this;
    }

    public @Nullable List<Object> getRawResponses() {
        return rawResponses;
    }

    public DiscoveryTask setRawResponses(@Nullable List<Object> rawResponses) {
        this.rawResponses = rawResponses;
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
                .add("result", result)
                .toString();
    }
}
