package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.annotation.Nullable;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_message_id", unique = true)
    private LogMessage logMessage;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "job")
    private List<DiscoveryTask> tasks = new ArrayList<>();

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

    public @Nullable Genre getGenre() {
        return genre;
    }

    public DiscoveryJob setGenre(@Nullable Genre genre) {
        this.genre = genre;
        return this;
    }

    public @Nullable Artist getArtist() {
        return artist;
    }

    public DiscoveryJob setArtist(@Nullable Artist artist) {
        this.artist = artist;
        return this;
    }

    public @Nullable Album getAlbum() {
        return album;
    }

    public DiscoveryJob setAlbum(@Nullable Album album) {
        this.album = album;
        return this;
    }

    public @Nullable Song getSong() {
        return song;
    }

    public DiscoveryJob setSong(@Nullable Song song) {
        this.song = song;
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("updateDate", updateDate)
                .add("type", type)
                .add("status", status)
                .toString();
    }
}
