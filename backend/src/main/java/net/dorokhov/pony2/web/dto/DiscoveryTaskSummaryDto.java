package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;

import java.time.Duration;
import java.time.LocalDateTime;

public final class DiscoveryTaskSummaryDto extends BaseDto<DiscoveryTaskSummaryDto> {

    private DiscoveryTask.Status status;
    private DiscoveryTaskType type;
    private Long duration;
    private String targetId;
    private ArtistDto artist;
    private AlbumDto album;

    public DiscoveryTask.Status getStatus() {
        return status;
    }

    public DiscoveryTaskSummaryDto setStatus(DiscoveryTask.Status status) {
        this.status = status;
        return this;
    }

    public DiscoveryTaskType getType() {
        return type;
    }

    public DiscoveryTaskSummaryDto setType(DiscoveryTaskType type) {
        this.type = type;
        return this;
    }

    @Nullable
    public Long getDuration() {
        return duration;
    }

    public DiscoveryTaskSummaryDto setDuration(@Nullable Long duration) {
        this.duration = duration;
        return this;
    }

    public String getTargetId() {
        return targetId;
    }

    public DiscoveryTaskSummaryDto setTargetId(String targetId) {
        this.targetId = targetId;
        return this;
    }

    @Nullable
    public ArtistDto getArtist() {
        return artist;
    }

    public DiscoveryTaskSummaryDto setArtist(@Nullable ArtistDto artist) {
        this.artist = artist;
        return this;
    }

    @Nullable
    public AlbumDto getAlbum() {
        return album;
    }

    public DiscoveryTaskSummaryDto setAlbum(@Nullable AlbumDto album) {
        this.album = album;
        return this;
    }

    public static DiscoveryTaskSummaryDto of(DiscoveryTask task, String targetId, @Nullable Artist artist, @Nullable Album album) {
        LocalDateTime endDate = task.getStatus() == DiscoveryTask.Status.STARTED ? LocalDateTime.now() : task.getUpdateDate();
        return new DiscoveryTaskSummaryDto()
                .setId(task.getId())
                .setCreationDate(task.getCreationDate())
                .setUpdateDate(task.getUpdateDate())
                .setStatus(task.getStatus())
                .setType(task.getType())
                .setDuration(endDate != null ? Duration.between(task.getCreationDate(), endDate).toSeconds() : null)
                .setTargetId(targetId)
                .setArtist(artist != null ? ArtistDto.of(artist) : null)
                .setAlbum(album != null ? AlbumDto.of(album) : null);
    }
}
