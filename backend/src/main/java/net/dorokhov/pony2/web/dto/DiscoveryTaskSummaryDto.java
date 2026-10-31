package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;

import java.time.Duration;
import java.time.LocalDateTime;

public final class DiscoveryTaskSummaryDto extends BaseDto<DiscoveryTaskSummaryDto> {

    private DiscoveryTask.Status status;
    private boolean noResult;
    private DiscoveryTaskType type;
    private Long duration;
    private GenreDto genre;
    private ArtistDto artist;
    private AlbumDto album;
    private SongDto song;

    public DiscoveryTask.Status getStatus() {
        return status;
    }

    public DiscoveryTaskSummaryDto setStatus(DiscoveryTask.Status status) {
        this.status = status;
        return this;
    }

    public boolean isNoResult() {
        return noResult;
    }

    public DiscoveryTaskSummaryDto setNoResult(boolean noResult) {
        this.noResult = noResult;
        return this;
    }

    public DiscoveryTaskType getType() {
        return type;
    }

    public DiscoveryTaskSummaryDto setType(DiscoveryTaskType type) {
        this.type = type;
        return this;
    }

    public @Nullable Long getDuration() {
        return duration;
    }

    public DiscoveryTaskSummaryDto setDuration(@Nullable Long duration) {
        this.duration = duration;
        return this;
    }

    public @Nullable GenreDto getGenre() {
        return genre;
    }

    public DiscoveryTaskSummaryDto setGenre(@Nullable GenreDto genre) {
        this.genre = genre;
        return this;
    }

    public @Nullable ArtistDto getArtist() {
        return artist;
    }

    public DiscoveryTaskSummaryDto setArtist(@Nullable ArtistDto artist) {
        this.artist = artist;
        return this;
    }

    public @Nullable AlbumDto getAlbum() {
        return album;
    }

    public DiscoveryTaskSummaryDto setAlbum(@Nullable AlbumDto album) {
        this.album = album;
        return this;
    }

    public @Nullable SongDto getSong() {
        return song;
    }

    public DiscoveryTaskSummaryDto setSong(@Nullable SongDto song) {
        this.song = song;
        return this;
    }

    public static DiscoveryTaskSummaryDto of(DiscoveryTask task, @Nullable GenreDto genre, @Nullable ArtistDto artist,
                                            @Nullable AlbumDto album, @Nullable SongDto song) {
        LocalDateTime endDate = task.getStatus() == DiscoveryTask.Status.STARTED ? LocalDateTime.now() : task.getUpdateDate();
        return new DiscoveryTaskSummaryDto()
                .setId(task.getId())
                .setCreationDate(task.getCreationDate())
                .setUpdateDate(task.getUpdateDate())
                .setStatus(task.getStatus())
                .setNoResult(task.getStatus() == DiscoveryTask.Status.COMPLETE && task.getResult() == null)
                .setType(task.getType())
                .setDuration(endDate != null ? Duration.between(task.getCreationDate(), endDate).toSeconds() : null)
                .setGenre(genre)
                .setArtist(artist)
                .setAlbum(album)
                .setSong(song);
    }
}
