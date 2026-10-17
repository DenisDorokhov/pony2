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

    public record ArtistParameter(String artistId) {}

    public record AlbumParameter(String albumId) {}

    public record SpotifyTopTracksParameter(String artistId, String spotifyArtistDataTaskId) {}

    public record ErrorResult(String error) {}

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @NotNull
    private Status status;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @NotNull
    private DiscoveryTaskType type;

    @Column(name = "parameter")
    @NotNull
    private String parameter;

    @Column(name = "result")
    private String result;

    @Column(name = "raw_request")
    private String rawRequest;

    @Column(name = "raw_result")
    private String rawResult;

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

    @Nullable
    public Genre getGenre() {
        return genre;
    }

    public DiscoveryTask setGenre(@Nullable Genre genre) {
        this.genre = genre;
        return this;
    }

    @Nullable
    public Artist getArtist() {
        return artist;
    }

    public DiscoveryTask setArtist(@Nullable Artist artist) {
        this.artist = artist;
        return this;
    }

    @Nullable
    public Album getAlbum() {
        return album;
    }

    public DiscoveryTask setAlbum(@Nullable Album album) {
        this.album = album;
        return this;
    }

    @Nullable
    public Song getSong() {
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

    public String getParameter() {
        return parameter;
    }

    public DiscoveryTask setParameter(String parameter) {
        this.parameter = parameter;
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

    @Nullable
    public String getRawRequest() {
        return rawRequest;
    }

    public DiscoveryTask setRawRequest(@Nullable String rawRequest) {
        this.rawRequest = rawRequest;
        return this;
    }

    @Nullable
    public String getRawResult() {
        return rawResult;
    }

    public DiscoveryTask setRawResult(@Nullable String rawResult) {
        this.rawResult = rawResult;
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
                .add("parameter", parameter)
                .add("result", result)
                .toString();
    }
}
